/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.dyq.bletest.common.heartRate;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import com.dyq.bletest.R;
import com.dyq.bletest.common.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;


/**
 * <p>The BleManager is responsible for managing the low level communication with a Bluetooth Smart device. Please see profiles implementation for an example of use.
 * This base manager has been tested against number of devices and samples from Nordic SDK.</p>
 * <p>The manager handles connection events and initializes the device after establishing the connection.
 * <ol>
 * <li>For bonded devices it ensures that the Service Changed indications, if this characteristic is present, are enabled. Android does not enable them by default,
 * leaving this to the developers.</li>
 * <li>The manager tries to read the Battery Level characteristic. No matter the result of this operation (for example the Battery Level characteristic may not have the READ property)
 * it tries to enable Battery Level notifications, to get battery updates from the device.</li>
 * <li>Afterwards, the manager initializes the device using given queue of commands. See {@link BleManagerGattCallback#initGatt(BluetoothGatt)} method for more details.</li>
 * <li>When initialization complete, the {@link BleManagerCallbacks#onDeviceReady()} callback is called.</li>
 * </ol>The manager also is responsible for parsing the Battery Level values and calling {@link BleManagerCallbacks# onBatteryValueReceived(int)} method.</p>
 * <p>Events from all profiles are being logged into the nRF Logger application,
 * which may be downloaded from Google Play: <a href="https://play.google.com/store/apps/details?id=no.nordicsemi.android.log">https://play.google.com/store/apps/details?id=no.nordicsemi.android.log</a></p>
 * <p>The nRF Logger application allows you to see application logs without need to connect it to the computer.</p>
 *
 * @param <E> The profile callbacks type
 */
public abstract class BleManager<E extends BleManagerCallbacks> {
	private final static String TAG = "BleManager";

	private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	private final static UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
	private final static UUID BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

	private final static UUID GENERIC_ATTRIBUTE_SERVICE = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
	private final static UUID SERVICE_CHANGED_CHARACTERISTIC = UUID.fromString("00002A05-0000-1000-8000-00805f9b34fb");

	private final static String ERROR_CONNECTION_STATE_CHANGE = "Error on connection state change";
	private final static String ERROR_DISCOVERY_SERVICE = "Error on discovering services";
	private final static String ERROR_AUTH_ERROR_WHILE_BONDED = "Phone has lost bonding information";
	private final static String ERROR_WRITE_DESCRIPTOR = "Error on writing descriptor";
	private final static String ERROR_READ_CHARACTERISTIC = "Error on reading characteristic";
	private final static String ERROR_WRITE_CHARCTERISTIC = "Error on writing characteristic";

	/**
	 * The log session or null if nRF Logger is not installed.
	 */
//	protected ILogSession mLogSession;
	protected E mCallbacks;
	private Handler mHandler;
	private List<BluetoothGatt> mBluetoothGatt;
	private Context mContext;
	private boolean mUserDisconnected;
//	private boolean mConnected;


	public List<BluetoothGatt> getmBluetoothGatt() {
		return mBluetoothGatt;
	}

	private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
				final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

				// Skip other devices
				if(mBluetoothGatt == null|| mBluetoothGatt.size() == 0){
					return;
				}
				boolean haveInclude = false;
				int index = -1;
				for(int i = 0;i< mBluetoothGatt.size();i++) {
					if (device.getAddress().equals(mBluetoothGatt.get(i).getDevice().getAddress())) {
						index = i;
						haveInclude = true;
					}
				}
				if(!haveInclude){
					return;
				}

				switch (bondState) {
					case BluetoothDevice.BOND_BONDING:
						mCallbacks.onBondingRequired();
						break;
					case BluetoothDevice.BOND_BONDED:
//					Logger.i(mLogSession, "Device bonded");
						mCallbacks.onBonded();

						// Start initializing again.
						// In fact, bonding forces additional, internal service discovery (at least on Nexus devices), so this method may safely be used to start this process again.
//					Logger.v(mLogSession, "Discovering Services...");
//					Logger.d(mLogSession, "gatt.discoverServices()");
						if(index!=-1) {
							mBluetoothGatt.get(index).discoverServices();
						}
						break;
				}
			}
		}
	};

	private final BroadcastReceiver mPairingRequestBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {}
	};

	public BleManager(final Context context) {
		mContext = context;
		mHandler = new Handler();
		mUserDisconnected = false;
		mBluetoothGatt = new ArrayList<>();
		// Register bonding broadcast receiver
		context.registerReceiver(mBondingBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
		context.registerReceiver(mPairingRequestBroadcastReceiver, new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST"/*BluetoothDevice.ACTION_PAIRING_REQUEST*/));
	}

	/**
	 * Returns the context that the manager was created with.
	 *
	 * @return the context
	 */
	protected Context getContext() {
		return mContext;
	}

	/**
	 * This method must return the gatt callback used by the manager.
	 * This method must not create a new gatt callback each time it is being invoked, but rather return a single object.
	 *
	 * @return the gatt callback object
	 */
	protected abstract BleManagerGattCallback getGattCallback();

	/**
	 * Returns whether to directly connect to the remote device (false) or to automatically connect as soon as the remote
	 * device becomes available (true).
	 *
	 * @return autoConnect flag value
	 */
	protected boolean shouldAutoConnect() {
		return false;
	}

	/**
	 * Connects to the Bluetooth Smart device
	 *
	 * @param device a device to connect to
	 */
	public void connect(final BluetoothDevice device) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			Logger.i(Logger.DEBUG_TAG,"BleManager,connect(),Address:"+device.getAddress());
//			if (mConnected)//防止多联的情况
//				return;
//			if (mBluetoothGatt != null) {
//				mBluetoothGatt.close();
//				mBluetoothGatt = null;
//			}

//			final boolean autoConnect = shouldAutoConnect();
//			mUserDisconnected = !autoConnect; // We will receive Linkloss events only when the device is connected with autoConnect=true
			mUserDisconnected = true;
			boolean haveExistGatt = false;
			for(int i =0;i<mBluetoothGatt.size();i++){
				String macAddress = mBluetoothGatt.get(i).getDevice().getAddress();
				if(macAddress.equals(device.getAddress())){
					Logger.i(Logger.DEBUG_TAG,"connect(),存在相同macAddress:"+macAddress+"的mBluetoothGatt");
					haveExistGatt = true;
//					mBluetoothGatt.get(i).disconnect();
//					mBluetoothGatt.get(i).close();
//					mBluetoothGatt.remove(i);
					break;
				}
			}

			BluetoothGatt gatt = device.connectGatt(mContext, false, getGattCallback());//TODO 暂时弄成自动重连设置
			if(gatt!=null) {
				if(!haveExistGatt) {
					mBluetoothGatt.add(gatt);
				}else{
					gatt = null;
				}
			}
		}else{
			Toast.makeText(getContext(), getContext().getString(R.string.heart_rate_warn_sdk_version), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 断开全部连接
	 * @return
	 */
	public void disconnect(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			mUserDisconnected = true;

			if (mBluetoothGatt != null && mBluetoothGatt.size()>0) {

				for(int i =0;i<mBluetoothGatt.size();i++){
					BluetoothGatt gattt = mBluetoothGatt.get(i);
					if(mCallbacks!=	null) {
						mCallbacks.onDeviceDisconnecting(gattt.getDevice().getAddress());
					}
					gattt.disconnect();
				}
			}
		}
	}
	/**
	 * Disconnects from the device. Does nothing if not connected.
	 * @return true if device is to be disconnected. False if it was already disconnected.
	 */
	public boolean disconnect(BluetoothGatt gatt) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			mUserDisconnected = true;

			if (mBluetoothGatt != null && mBluetoothGatt.size()>0) {
				mCallbacks.onDeviceDisconnecting(gatt.getDevice().getAddress());
				for(int i =0;i<mBluetoothGatt.size();i++){
					BluetoothGatt gattt = mBluetoothGatt.get(i);
					if(gatt.getDevice().getAddress().equals(gattt.getDevice().getAddress())){
						gattt.disconnect();
						return true;
					}
				}
			}
			return false;
		}
		return true;
	}
	/**
	 * Disconnects from the device. Does nothing if not connected.
	 * @return true if device is to be disconnected. False if it was already disconnected.
	 */
	public boolean disconnect(String deviceName) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			mUserDisconnected = true;
				mCallbacks.onDeviceDisconnecting(deviceName);
				for(int i =0;i<mBluetoothGatt.size();i++){
					BluetoothGatt gattt = mBluetoothGatt.get(i);
					if(deviceName.equals(gattt.getDevice().getAddress())){
						gattt.disconnect();
						return true;
					}
				}
			return false;
		}
		return true;
	}

	/**
	 * Closes and releases resources. May be also used to unregister broadcast listeners.
	 */
	public void close() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			try {
				mContext.unregisterReceiver(mBondingBroadcastReceiver);
				mContext.unregisterReceiver(mPairingRequestBroadcastReceiver);
			} catch (Exception e) {
				// the receiver must have been not registered or unregistered before
			}

			if(getGattCallback()!=null) {
				getGattCallback().onDeviceDisconnected();
			}

			if (mBluetoothGatt != null && mBluetoothGatt.size()>0) {
				for(int i=0;i<mBluetoothGatt.size();i++) {
					mBluetoothGatt.get(i).close();
				}
//				mBluetoothGatt = null;
			}
			mUserDisconnected = false;
		}
	}

//	/**
//	 * Sets the optional log session. This session will be used to log Bluetooth events.
//	 * The logs may be viewed using the nRF Logger application: https://play.google.com/store/apps/details?id=no.nordicsemi.android.log
//	 * Since nRF Logger Library v2.0 an app may define it's own log provider. Use @link BleProfileServiceReadyActivity#getLocalAuthorityLogger() to define local log URI.
//	 * NOTE: nRF Logger must be installed prior to nRF Toolbox as it defines the required permission which is used by nRF Toolbox.
//	 *
//	 * @param session the session, or null if nRF Logger is not installed.
//	 */
//	public void setLogger(final ILogSession session) {
//		mLogSession = session;
//	}

	/**
	 * 发送心率耳机消息指令操作
	 * @param msgType
	 */
	public void sendHRCmd(String macAddress,int msgType){
		//TODO 注意释放资源，以防内存泄露
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			byte[] bytesMsg = new byte[0];
			boolean byteInit = false;
			switch (msgType) {
				case -1 :
					bytesMsg = new byte[]{0x00};//停止心率检测
					byteInit = true;
					break;
				case 0 :
					bytesMsg = new byte[]{0x01};//开启心率检测
					byteInit = true;
					break;
				case 1:
					bytesMsg = new byte[]{0x02};//心率模式
					byteInit = true;
					break;
				case 2:
					bytesMsg = new byte[]{0x03};//呼吸模式
					byteInit = true;
					break;
				case 3:
					bytesMsg = new byte[]{0x04};//关闭灯光
					byteInit = true;
					break;
				case 4:
					bytesMsg = new byte[]{0x05};//清除统计数据
					byteInit = true;
					break;
				case 5:
					bytesMsg = new byte[]{0x06};//常亮灯光模式
					byteInit = true;
					break;
			}
			if(!byteInit){
				return;
			}
			if(mBluetoothGatt!=null) {
				int index = -1;
				for(int i =0;i<mBluetoothGatt.size();i++){
					if(mBluetoothGatt.get(i).getDevice().getAddress().equals(macAddress)){
						index = i;
						break;
					}
				}
				if(index  == -1){
					return;
				}
				BluetoothGattService mBluetoothService = mBluetoothGatt.get(index).getService(HRSManager.HR_SERVICE_BlUETOOTH_EARPHONE_UUID);
				if (mBluetoothService != null) {
					BluetoothGattCharacteristic cmdCharacteristic = mBluetoothService.getCharacteristic(HRSManager.HR_SEND_CMD_CHARACTERISTIC_UUID);
					if (cmdCharacteristic != null) {
						cmdCharacteristic.setValue(bytesMsg);
						mBluetoothGatt.get(index).writeCharacteristic(cmdCharacteristic);
						getLightType(macAddress,msgType);
					}
				}
			}
		}
	}
	private void getLightType(String macAddress, int msgType){
		String lightType = "";
		switch (msgType){
			case -1 :
				lightType = "停止心率检测";
				break;
			case 0 :
				lightType = "开启心率检测";
				break;
			case 1:
				lightType = "心率";
				break;
			case 2:
				lightType = "呼吸";
				break;
			case 3:
				lightType = "关闭灯光";
				break;
			case 4:
				lightType = "清除统计数据";
				break;
			case 5:
				lightType = "常亮";
				break;
		}
		Logger.i(Logger.DEBUG_TAG,macAddress+"设置灯光:"+lightType);
	}

//	/**
//	 *	发送指令设置用户的基本信息用于统计运动数据
//	 *@param age    年龄,单位为月,范围:60 - 1440,默认:360（30岁）
//	 * @param sexuality 性别,0:女,1:男,默认:1（男）
//	 * @param weight 体重 单位:0.1kg,范围:100 - 5000,默认:816（81.6kg）
//	 * @param height 身高 单位:cm,范围:60 - 250,默认:180（180cm）
//	 * @param rest_hr    静息心率 单位:BPM,范围:1 - 220,默认:72（72BPM）
//	 * @param sport_mode   运动模式 0:autonomous mode,1:Running,2:Low HR,3:Cycling,4:Weights & Sports,5:Aerobics,6:Lifestyle,默认:1（Running）
//	 */
//	public void setUserData(int age,int sexuality, int weight, int height, int rest_hr, int sport_mode){
//		//TODO 注意释放资源，以防内存泄露
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//			if(mBluetoothGatt!=null) {
//				BluetoothGattService mBluetoothService = mBluetoothGatt.getService(HRSManager.HR_SERVICE_BlUETOOTH_EARPHONE_UUID);
//				if (mBluetoothService != null) {
//					BluetoothGattCharacteristic cmdCharacteristic = mBluetoothService.getCharacteristic(HRSManager.UR_USER_DATA_SET_CHARACTERISTIC_UUID);
//					if (cmdCharacteristic != null) {
//
//						cmdCharacteristic.setValue(getByteArrayByValues(age,sexuality,weight,height,rest_hr,sport_mode));
//						mBluetoothGatt.writeCharacteristic(cmdCharacteristic);
//					}
//				}
//			}
//		}
//	}

	/**
	 * 根据参数值获取与协议对应的字节数组
	 *
	 * @param age    年龄,单位为月,范围:60 - 1440,默认:360（30岁）
	 * @param gender 性别,0:女,1:男,默认:1（男）
	 * @param weight 体重 单位:0.1kg,范围:100 - 5000,默认:816（81.6kg）
	 * @param height 身高 单位:cm,范围:60 - 250,默认:180（180cm）
	 * @param bpm    静息心率 单位:BPM,范围:1 - 220,默认:72（72BPM）
	 * @param mode   运动模式 0:autonomous mode,1:Running,2:Low HR,3:Cycling,4:Weights & Sports,5:Aerobics,6:Lifestyle,默认:1（Running）
	 */
	public byte[] getByteArrayByValues(int age, int gender, int weight, int height, int bpm, int mode) {
		byte[] gdata = new byte[12];
		gdata[0] = (byte) (age >> 8);
		gdata[1] = (byte) age;
		gdata[2] = (byte) (gender >> 8);
		gdata[3] = (byte) gender;
		gdata[4] = (byte) (weight >> 8);
		gdata[5] = (byte) weight;
		gdata[6] = (byte) (height >> 8);
		gdata[7] = (byte) height;
		gdata[8] = (byte) (bpm >> 8);
		gdata[9] = (byte) bpm;
		gdata[10] = (byte) (mode >> 8);
		gdata[11] = (byte) mode;
		return gdata;
	}

	/**
	 * Sets the manager callback listener
	 *
	 * @param callbacks the callback listener
	 */
	public void setGattCallbacks(E callbacks) {
		mCallbacks = callbacks;
	}

	/**
	 * Returns true if this descriptor is from the Service Changed characteristic.
	 *
	 * @param descriptor the descriptor to be checked
	 * @return true if the descriptor belongs to the Service Changed characteristic
	 */
	private boolean isServiceChangedCCCD(final BluetoothGattDescriptor descriptor) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			if (descriptor == null)
				return false;

			return SERVICE_CHANGED_CHARACTERISTIC.equals(descriptor.getCharacteristic().getUuid());
		}
		return false;
	}

	/**
	 * Returns true if the characteristic is the Battery Level characteristic.
	 *
	 * @param characteristic the characteristic to be checked
	 * @return true if the characteristic is the Battery Level characteristic.
	 */
	private boolean isBatteryLevelCharacteristic(final BluetoothGattCharacteristic characteristic) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			if (characteristic == null)
				return false;
			return BATTERY_LEVEL_CHARACTERISTIC.equals(characteristic.getUuid());
		}
		return false;
	}

	/**
	 * Returns true if this descriptor is from the Battery Level characteristic.
	 *
	 * @param descriptor the descriptor to be checked
	 * @return true if the descriptor belongs to the Battery Level characteristic
	 */
	private boolean isBatteryLevelCCCD(final BluetoothGattDescriptor descriptor) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			if (descriptor == null)
				return false;
			return BATTERY_LEVEL_CHARACTERISTIC.equals(descriptor.getCharacteristic().getUuid());
		}
		return false;

	}

	/**
	 * When the device is bonded and has the Generic Attribute service and the Service Changed characteristic this method enables indications on this characteristic.
	 * In case one of the requirements is not fulfilled this method returns <code>false</code>.
	 *
	 * @param gatt the gatt device with services discovered
	 * @return <code>true</code> when the request has been sent, <code>false</code> when the device is not bonded, does not have the Generic Attribute service, the GA service does not have
	 * the Service Changed characteristic or this characteristic does not have the CCCD.
	 */
	private boolean ensureServiceChangedEnabled(final BluetoothGatt gatt) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			if (gatt == null)
				return false;

			// The Service Changed indications have sense only on bonded devices
			final BluetoothDevice device = gatt.getDevice();
			if (device.getBondState() != BluetoothDevice.BOND_BONDED)
				return false;

			final BluetoothGattService gaService = gatt.getService(GENERIC_ATTRIBUTE_SERVICE);
			if (gaService == null)
				return false;

			final BluetoothGattCharacteristic scCharacteristic = gaService.getCharacteristic(SERVICE_CHANGED_CHARACTERISTIC);
			if (scCharacteristic == null)
				return false;

//		Logger.i(mLogSession, "Service Changed characteristic found on a bonded device");
			return enableIndications(gatt,scCharacteristic);
		}
		return false;

	}

	/**
	 * Enables notifications on given characteristic
	 *
	 * @return true is the request has been sent, false if one of the arguments was <code>null</code> or the characteristic does not have the CCCD.
	 */
	protected final boolean enableNotifications(BluetoothGatt theBlueToothGatt,final BluetoothGattCharacteristic characteristic) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			BluetoothGatt gatt = null;
			for(int i =0 ;i<mBluetoothGatt.size();i++) {
				BluetoothGatt gattt = mBluetoothGatt.get(i);
				if(gattt.getDevice().getAddress().equals(theBlueToothGatt.getDevice().getAddress())) {
					gatt = gattt;
				}
			}
			if (gatt == null || characteristic == null)
				return false;

			// Check characteristic property
			final int properties = characteristic.getProperties();
			if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
				return false;

//		Logger.d(mLogSession, "gatt.setCharacteristicNotification(" + characteristic.getUuid() + ", true)");
			gatt.setCharacteristicNotification(characteristic, true);
			final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
			if (descriptor != null) {
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//			Logger.v(mLogSession, "Enabling notifications for " + characteristic.getUuid());
//			Logger.d(mLogSession, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID + ", value=0x01-00)");
				return gatt.writeDescriptor(descriptor);
			}
		}
		return false;
	}

	/**
	 * Enables indications on given characteristic
	 *
	 * @return true is the request has been sent, false if one of the arguments was <code>null</code> or the characteristic does not have the CCCD.
	 */
	protected final boolean enableIndications(BluetoothGatt theBlueToothGatt,final BluetoothGattCharacteristic characteristic) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			BluetoothGatt gatt = null;
			for(int i =0 ;i<mBluetoothGatt.size();i++) {
				BluetoothGatt gattt = mBluetoothGatt.get(i);
				if(gattt.getDevice().getAddress().equals(theBlueToothGatt.getDevice().getAddress())) {
					gatt = gattt;
				}
			}

			if (gatt == null || characteristic == null)
				return false;

			// Check characteristic property
			final int properties = characteristic.getProperties();
			if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0)
				return false;

//		Logger.d(mLogSession, "gatt.setCharacteristicNotification(" + characteristic.getUuid() + ", true)");
			gatt.setCharacteristicNotification(characteristic, true);
			final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
			if (descriptor != null) {
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
//			Logger.v(mLogSession, "Enabling indications for " + characteristic.getUuid());
//			Logger.d(mLogSession, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID + ", value=0x02-00)");
				return gatt.writeDescriptor(descriptor);
			}
		}
		return false;
	}

	/**
	 * Sends the read request to the given characteristic.
	 *
	 * @param characteristic the characteristic to read
	 * @return true if request has been sent
	 */
	protected final boolean readCharacteristic(BluetoothGatt theBlueToothGatt,final BluetoothGattCharacteristic characteristic) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			BluetoothGatt gatt = null;
			for(int i =0 ;i<mBluetoothGatt.size();i++) {
				BluetoothGatt gattt = mBluetoothGatt.get(i);
				if(gattt.getDevice().getAddress().equals(theBlueToothGatt.getDevice().getAddress())) {
					gatt = gattt;
				}
			}

			if (gatt == null || characteristic == null)
				return false;

			// Check characteristic property
			final int properties = characteristic.getProperties();
			if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == 0)
				return false;

//		Logger.v(mLogSession, "Reading characteristic " + characteristic.getUuid());
//		Logger.d(mLogSession, "gatt.readCharacteristic(" + characteristic.getUuid() + ")");
			return gatt.readCharacteristic(characteristic);
		}
		return false;
	}

	/**
	 * Writes the characteristic value to the given characteristic.
	 *
	 * @param characteristic the characteristic to write to
	 * @return true if request has been sent
	 */
	protected final boolean writeCharacteristic(BluetoothGatt theBlueToothGatt,final BluetoothGattCharacteristic characteristic) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			BluetoothGatt gatt =null;
			for(int i =0;i<mBluetoothGatt.size();i++) {
				BluetoothGatt gattt = mBluetoothGatt.get(i);
				if(gattt.getDevice().getAddress().equals(theBlueToothGatt.getDevice().getAddress())) {
					gatt = gattt;
				}
			}
			if (gatt == null || characteristic == null)
				return false;

			// Check characteristic property
			final int properties = characteristic.getProperties();
			if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0)
				return false;

//		Logger.v(mLogSession, "Writing characteristic " + characteristic.getUuid() + " (" + getWriteType(characteristic.getWriteType()) + ")");
//		Logger.d(mLogSession, "gatt.writeCharacteristic(" + characteristic.getUuid() + ")");
			return gatt.writeCharacteristic(characteristic);
		}
		return false;
	}

	/**
	 * Reads the battery level from the device.
	 *
	 * @return true if request has been sent
	 */
	public final boolean readBatteryLevel(BluetoothGatt theBlueToothGatt) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			BluetoothGatt gatt =null;
			for(int i =0;i<mBluetoothGatt.size();i++) {
				BluetoothGatt gattt = mBluetoothGatt.get(i);
				if(gattt.getDevice().getAddress().equals(theBlueToothGatt.getDevice().getAddress())) {
					gatt = gattt;
				}
			}

			if (gatt == null)
				return false;

			final BluetoothGattService batteryService = gatt.getService(BATTERY_SERVICE);
			if (batteryService == null)
				return false;

			final BluetoothGattCharacteristic batteryLevelCharacteristic = batteryService.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC);
			if (batteryLevelCharacteristic == null)
				return false;

			// Check characteristic property
			final int properties = batteryLevelCharacteristic.getProperties();
			if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
				return setBatteryNotifications(gatt,true);
			}

//		Logger.a(mLogSession, "Reading battery level...");
			return readCharacteristic(gatt,batteryLevelCharacteristic);
		}
		return false;
	}

	/**
	 * This method tries to enable notifications on the Battery Level characteristic.
	 *
	 * @param enable <code>true</code> to enable battery notifications, false to disable
	 * @return true if request has been sent
	 */
	public boolean setBatteryNotifications(BluetoothGatt theBlueToothGatt,final boolean enable) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			BluetoothGatt gatt =null;
			for(int i =0;i<mBluetoothGatt.size();i++) {
				BluetoothGatt gattt = mBluetoothGatt.get(i);
				if(gattt.getDevice().getAddress().equals(theBlueToothGatt.getDevice().getAddress())) {
					gatt = gattt;
				}
			}
			if (gatt == null) {
				return false;
			}

			final BluetoothGattService batteryService = gatt.getService(BATTERY_SERVICE);
			if (batteryService == null)
				return false;

			final BluetoothGattCharacteristic batteryLevelCharacteristic = batteryService.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC);
			if (batteryLevelCharacteristic == null)
				return false;

			// Check characteristic property
			final int properties = batteryLevelCharacteristic.getProperties();
			if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
				return false;

			gatt.setCharacteristicNotification(batteryLevelCharacteristic, enable);
			final BluetoothGattDescriptor descriptor = batteryLevelCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
			if (descriptor != null) {
				if (enable) {
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//				Logger.a(mLogSession, "Enabling battery level notifications...");
//				Logger.v(mLogSession, "Enabling notifications for " + BATTERY_LEVEL_CHARACTERISTIC);
//				Logger.d(mLogSession, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID + ", value=0x01-00)");
				} else {
					descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
//				Logger.a(mLogSession, "Disabling battery level notifications...");
//				Logger.v(mLogSession, "Disabling notifications for " + BATTERY_LEVEL_CHARACTERISTIC);
//				Logger.d(mLogSession, "gatt.writeDescriptor(" + CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID + ", value=0x00-00)");
				}
				return gatt.writeDescriptor(descriptor);
			}
		}
		return false;
	}

	protected static final class Request {
		private enum Type {
			WRITE,
			READ,
			ENABLE_NOTIFICATIONS,
			ENABLE_INDICATIONS
		}

		private final Type type;
		private final BluetoothGattCharacteristic characteristic;
		private final byte[] value;

		private Request(final Type type, final BluetoothGattCharacteristic characteristic) {
			this.type = type;
			this.characteristic = characteristic;
			this.value = null;
		}

		private Request(final Type type, final BluetoothGattCharacteristic characteristic, final byte[] value) {
			this.type = type;
			this.characteristic = characteristic;
			this.value = value;
		}

		public static Request newReadRequest(final BluetoothGattCharacteristic characteristic) {
			return new Request(Type.READ, characteristic);
		}

		public static Request newWriteRequest(final BluetoothGattCharacteristic characteristic, final byte[] value) {
			return new Request(Type.WRITE, characteristic, value);
		}

		public static Request newEnableNotificationsRequest(final BluetoothGattCharacteristic characteristic) {
			return new Request(Type.ENABLE_NOTIFICATIONS, characteristic);
		}

		public static Request newEnableIndicationsRequest(final BluetoothGattCharacteristic characteristic) {
			return new Request(Type.ENABLE_INDICATIONS, characteristic);
		}
	}

	@SuppressLint("NewApi")
	protected abstract class BleManagerGattCallback extends BluetoothGattCallback {
		private Queue<Request> mInitQueue;
		private boolean mInitInProgress;

		/**
		 * This method should return <code>true</code> when the gatt device supports the required services.
		 *
		 * @param gatt the gatt device with services discovered
		 * @return <code>true</code> when the device has teh required service
		 */
		protected abstract boolean isRequiredServiceSupported(final BluetoothGatt gatt);

		/**
		 * This method should return <code>true</code> when the gatt device supports the optional services.
		 * The default implementation returns <code>false</code>.
		 *
		 * @param gatt the gatt device with services discovered
		 * @return <code>true</code> when the device has teh optional service
		 */
		protected boolean isOptionalServiceSupported(final BluetoothGatt gatt) {
			return false;
		}

		/**
		 * This method should return a list of requests needed to initialize the profile.
		 * Enabling Service Change indications for bonded devices and reading the Battery Level value and enabling Battery Level notifications
		 * is handled before executing this queue. The queue should not have requests that are not available, e.g. should not
		 * read an optional service when it is not supported by the connected device.
		 * <p>This method is called when the services has been discovered and the device is supported (has required service).</p>
		 *
		 * @param gatt the gatt device with services discovered
		 * @return the queue of requests
		 */
		protected abstract Queue<Request> initGatt(final BluetoothGatt gatt);

		/**
		 * Called then the initialization queue is complete.
		 */
		protected void onDeviceReady() {
			mCallbacks.onDeviceReady();
		}

		/**
		 * This method should nullify all services and characteristics of the device.
		 */
		protected abstract void onDeviceDisconnected();

		/**
		 * Callback reporting the result of a characteristic read operation.
		 *
		 * @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
		 * @param characteristic Characteristic that was read from the associated
		 *                       remote device.
		 */
		protected void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// do nothing
		}

		/**
		 * Callback indicating the result of a characteristic write operation.
		 * <p/>
		 * <p>If this callback is invoked while a reliable write transaction is
		 * in progress, the value of the characteristic represents the value
		 * reported by the remote device. An application should compare this
		 * value to the desired value to be written. If the values don't match,
		 * the application must abort the reliable write transaction.
		 *
		 * @param gatt           GATT client invoked {@link BluetoothGatt#writeCharacteristic}
		 * @param characteristic Characteristic that was written to the associated
		 *                       remote device.
		 */
		protected void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// do nothing
		}

		protected void onCharacteristicNotified(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// do nothing
		}

		protected void onCharacteristicIndicated(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// do nothing
		}

		private void onError(final String message, final int errorCode) {
			if(mCallbacks == null){
				return;
			}
			mCallbacks.onError(message, errorCode);
		}

		@Override
		public final void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
			if(mCallbacks == null){
				return;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
					// Notify the parent activity/service
					if(mCallbacks!=null) {
						mCallbacks.onDeviceConnected(gatt.getDevice());
					}
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
								if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_BONDING) {
									gatt.discoverServices();
								}
							}
						}
					}, 600);
				} else {
					if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//						if (status != BluetoothGatt.GATT_SUCCESS)

//							onDeviceDisconnected();//TODO 改为只有结束时才执行

//						mConnected = false;
//						if (mUserDisconnected) {


						for(int i =0;i<mBluetoothGatt.size();i++){
							BluetoothGatt gattt = mBluetoothGatt.get(i);
							String macAddress = gattt.getDevice().getAddress();
							if(macAddress.equals(gatt.getDevice().getAddress())){
								Logger.i(Logger.DEBUG_TAG,"onConnectionStateChange，STATE_DISCONNECTED,执行去除:"+macAddress);
								gattt.disconnect();
								gattt.close();
								mBluetoothGatt.remove(gattt);
								break;
							}
						}
						if(mCallbacks!=null) {
							mCallbacks.onDeviceDisconnected(gatt.getDevice());
						}

//							close();//TODO 不销毁
//						} else {
//							mCallbacks.onLinklossOccur();
							// We are not closing the connection here as the device should try to reconnect automatically.
							// This may be only called when the shouldAutoConnect() method returned true.
//						}
						return;
					}

					// TODO Should the disconnect method be called or the connection is still valid? Does this ever happen?
					if(mCallbacks!=null) {
						mCallbacks.onError(ERROR_CONNECTION_STATE_CHANGE, status);
					}
				}
			}
		}

		@Override
		public final void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
			if(mCallbacks == null){
				return;
			}
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (isRequiredServiceSupported(gatt)) {
					final boolean optionalServicesFound = isOptionalServiceSupported(gatt);
					if (optionalServicesFound)

					// Notify the parent activity
					mCallbacks.onServicesDiscovered(optionalServicesFound);

					// Obtain the queue of initialization requests
					mInitInProgress = true;
					mInitQueue = initGatt(gatt);

					// When the device is bonded and has Service Changed characteristic, the indications must be enabled first.
					// In case this method returns true we have to continue in the onDescriptorWrite callback
					if (ensureServiceChangedEnabled(gatt))
						return;

					// We have discovered services, let's start by reading the battery level value. If the characteristic is not readable, try to enable notifications.
					// If there is no Battery service, proceed with the initialization queue.
					if (!readBatteryLevel(gatt))
						nextRequest(gatt);
				} else {
					Logger.i(Logger.DEBUG_TAG,"BleManager...onServicesDiscovered(),isRequiredServiceSupported(gatt) == false");
					mCallbacks.onDeviceNotSupported();
					disconnect(gatt);
				}
			} else {
				onError(ERROR_DISCOVERY_SERVICE, status);
			}
		}

		@Override
		public final void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
			if(mCallbacks == null){
				return;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				if (status == BluetoothGatt.GATT_SUCCESS) {
					if (isBatteryLevelCharacteristic(characteristic)) {
//					final int batteryValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//					mCallbacks.onBatteryValueReceived(batteryValue);

						// The Battery Level value has been read. Let's try to enable Battery Level notifications.
						// If the Battery Level characteristic does not have the NOTIFY property, proceed with the initialization queue.
						if (!setBatteryNotifications(gatt,true))
							nextRequest(gatt);
					} else {
						// The value has been read. Notify the manager and proceed with the initialization queue.
						onCharacteristicRead(gatt, characteristic);
						nextRequest(gatt);
					}
				} else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
					if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_NONE) {
						mCallbacks.onError(ERROR_AUTH_ERROR_WHILE_BONDED, status);
					}
				} else {
					onError(ERROR_READ_CHARACTERISTIC, status);
				}
			}
		}

		@Override
		public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
			if(mCallbacks == null){
				return;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				/** 写特征值返回结果，status为写操作的结果			 */
//				Logger.i(Logger.DEBUG_TAG, "BleManager,onCharacteristicWrite(),status" + status);
				if (status == BluetoothGatt.GATT_SUCCESS) {
//				Logger.i(mLogSession, "Data written to " + characteristic.getUuid() + ", value: " + ParserUtils.parse(characteristic.getValue()));
					// The value has been written. Notify the manager and proceed with the initialization queue.
					onCharacteristicWrite(gatt, characteristic);
					nextRequest(gatt);
				} else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
					if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_NONE) {
						mCallbacks.onError(ERROR_AUTH_ERROR_WHILE_BONDED, status);
					}
				} else {
					onError(ERROR_WRITE_CHARCTERISTIC, status);
				}
			}
		}

		@Override
		public final void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
			if(mCallbacks == null){
				return;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				if (status == BluetoothGatt.GATT_SUCCESS) {
					if (isServiceChangedCCCD(descriptor)) {
//					Logger.a(mLogSession, "Service Changed notifications enabled");
						if (!readBatteryLevel(gatt))
							nextRequest(gatt);
					} else if (isBatteryLevelCCCD(descriptor)) {
						final byte[] value = descriptor.getValue();
						if (value != null && value.length > 0 && value[0] == 0x01) {
							nextRequest(gatt);
						} else {
						}
					} else {
						nextRequest(gatt);
					}
				} else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
					if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_NONE) {
						mCallbacks.onError(ERROR_AUTH_ERROR_WHILE_BONDED, status);
					}
				} else {
					onError(ERROR_WRITE_DESCRIPTOR, status);
				}
			}
		}

		@Override
		public final void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			if(mCallbacks == null){
				return;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//				final String data = ParserUtils.parse(characteristic);

				if (isBatteryLevelCharacteristic(characteristic)) {
//				final int batteryValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//				Logger.a(mLogSession, "Battery level received: " + batteryValue + "%");
//				mCallbacks.onBatteryValueReceived(batteryValue);
				} else {
					final BluetoothGattDescriptor cccd = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
					final boolean notifications = cccd == null || cccd.getValue() == null || cccd.getValue().length != 2 || cccd.getValue()[0] == 0x01;

					if (notifications) {
						onCharacteristicNotified(gatt, characteristic);
					} else { // indications
						onCharacteristicIndicated(gatt, characteristic);
					}
				}
			}
		}

		/**
		 * Executes the next initialization request. If the last element from the queue has been executed a {@link #onDeviceReady()} callback is called.
		 */
		private void nextRequest(BluetoothGatt gatt) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				final Queue<Request> requests = mInitQueue;

				// Get the first request from the queue
				final Request request = requests != null ? requests.poll() : null;

				// Are we done?
				if (request == null) {
					if (mInitInProgress) {
						mInitInProgress = false;
						onDeviceReady();
					}
					return;
				}

				switch (request.type) {
					case READ: {
						readCharacteristic(gatt,request.characteristic);
						break;
					}
					case WRITE: {
						final BluetoothGattCharacteristic characteristic = request.characteristic;
						characteristic.setValue(request.value);
						writeCharacteristic(gatt,characteristic);
						break;
					}
					case ENABLE_NOTIFICATIONS: {
						enableNotifications(gatt,request.characteristic);
						break;
					}
					case ENABLE_INDICATIONS: {
						enableIndications(gatt,request.characteristic);
						break;
					}
				}
			}
		}
	}

	private static final int PAIRING_VARIANT_PIN = 0;
	private static final int PAIRING_VARIANT_PASSKEY = 1;
	private static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
	private static final int PAIRING_VARIANT_CONSENT = 3;
	private static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;
	private static final int PAIRING_VARIANT_DISPLAY_PIN = 5;
	private static final int PAIRING_VARIANT_OOB_CONSENT = 6;

	protected String pairingVariantToString(final int variant) {
		switch (variant) {
			case PAIRING_VARIANT_PIN:
				return "PAIRING_VARIANT_PIN";
			case PAIRING_VARIANT_PASSKEY:
				return "PAIRING_VARIANT_PASSKEY";
			case PAIRING_VARIANT_PASSKEY_CONFIRMATION:
				return "PAIRING_VARIANT_PASSKEY_CONFIRMATION";
			case PAIRING_VARIANT_CONSENT:
				return "PAIRING_VARIANT_CONSENT";
			case PAIRING_VARIANT_DISPLAY_PASSKEY:
				return "PAIRING_VARIANT_DISPLAY_PASSKEY";
			case PAIRING_VARIANT_DISPLAY_PIN:
				return "PAIRING_VARIANT_DISPLAY_PIN";
			case PAIRING_VARIANT_OOB_CONSENT:
				return "PAIRING_VARIANT_OOB_CONSENT";
			default:
				return "UNKNOWN";
		}
	}

	protected String bondStateToString(final int state) {
		switch (state) {
			case BluetoothDevice.BOND_NONE:
				return "BOND_NONE";
			case BluetoothDevice.BOND_BONDING:
				return "BOND_BONDING";
			case BluetoothDevice.BOND_BONDED:
				return "BOND_BONDED";
			default:
				return "UNKNOWN";
		}
	}

	protected String getWriteType(final int type) {
		switch (type) {
			case BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT:
				return "WRITE REQUEST";
			case BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE:
				return "WRITE COMMAND";
			case BluetoothGattCharacteristic.WRITE_TYPE_SIGNED:
				return "WRITE SIGNED";
			default:
				return "UNKNOWN: " + type;
		}
	}

	/**
	 * Converts the connection state to String value
	 * @param state the connection state
	 * @return state as String
	 */
	protected String stateToString(final int state) {
		switch (state) {
			case BluetoothProfile.STATE_CONNECTED:
				return "CONNECTED";
			case BluetoothProfile.STATE_CONNECTING:
				return "CONNECTING";
			case BluetoothProfile.STATE_DISCONNECTING:
				return "DISCONNECTING";
			default:
				return "DISCONNECTED";
		}
	}
}
