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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;

import com.dyq.bletest.R;
import com.dyq.bletest.common.Logger;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;


/**
 * HRSManager class performs BluetoothGatt operations for connection, service discovery, enabling notification and reading characteristics. All operations required to connect to device with BLE HR
 * Service and reading heart rate values are performed here. HRSActivity implements HRSManagerCallbacks in order to receive callbacks of BluetoothGatt operations
 */
public class HRSManager extends BleManager<HRSManagerCallbacks> {
	public final static UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
	private static final UUID HR_SENSOR_LOCATION_CHARACTERISTIC_UUID = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");
	private static final UUID HR_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");


	private BluetoothGattCharacteristic mHRCharacteristic;
//	, mHRLocationCharacteristic

	/** FITMIX 心率耳机数据通道的BLE服务UUID */
	public final static UUID HR_SERVICE_BlUETOOTH_EARPHONE_UUID = UUID.fromString("87FE1523-A797-45FA-8AD7-2334BAE4CAF1");

	/** 接收到按键的事件 */
	private final static UUID HR_Button_CHARACTERISTIC_UUID = UUID.fromString("87FE1524-A797-45FA-8AD7-2334BAE4CAF1");
	/** 发送指令	 */
	public final static UUID HR_SEND_CMD_CHARACTERISTIC_UUID = UUID.fromString("87FE1525-A797-45FA-8AD7-2334BAE4CAF1");
	/** 接收到心率的相关数据	 */
	private final static UUID HR_DATA_CHARACTERISTIC_UUID = UUID.fromString("87FE1526-A797-45FA-8AD7-2334BAE4CAF1");

	/**
	 * 统计运动数据所需的参数设置
	 */
	public final static UUID UR_USER_DATA_SET_CHARACTERISTIC_UUID = UUID.fromString("87FE1527-A797-45FA-8AD7-2334BAE4CAF1");

	/**
	 * 运动数据
	 */
	private final static UUID SPORT_DATA_CHARACTERISTIC_UUID = UUID.fromString("87FE1528-A797-45FA-8AD7-2334BAE4CAF1");


	/** 心率耳机BLE服务特征值接收对象  */
	private BluetoothGattCharacteristic mHRBtnCharacteristic,mHRDataCharacteristic, mUserSportDataCharacteristic;
//	private BluetoothGattCharacteristic mHRSendCMDCharacteristic;


	private static HRSManager managerInstance = null;

	/**
	 * singleton implementation of HRSManager class
	 */
	public static synchronized HRSManager getInstance(final Context context) {
		if (managerInstance == null) {
			managerInstance = new HRSManager(context);
		}
		return managerInstance;
	}

	public HRSManager(final Context context) {
		super(context);
	}

	@Override
	protected BleManagerGattCallback getGattCallback() {
		return mGattCallback;
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving notification, etc
	 */
	private BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

		@Override
		protected Queue<Request> initGatt(final BluetoothGatt gatt) {
			final LinkedList<Request> requests = new LinkedList<>();
//			if (mHRLocationCharacteristic != null)
//				requests.push(Request.newReadRequest(mHRLocationCharacteristic));
			requests.add(Request.newEnableNotificationsRequest(mHRCharacteristic));
			requests.add(Request.newEnableNotificationsRequest(mHRBtnCharacteristic));
//			requests.add(Request.newEnableNotificationsRequest(mHRSendCMDCharacteristic));//心率耳机 按键事件
			requests.add(Request.newEnableNotificationsRequest(mHRDataCharacteristic));
//			requests.add(Request.newWriteRequest(mHRSendCMDCharacteristic, new byte[]{0x04}));//关闭灯光

//			requests.add(Request.newEnableIndicationsRequest(mUserSportDataCharacteristic));
			requests.add(Request.newEnableNotificationsRequest(mUserSportDataCharacteristic));//用户运动数据
			return requests;
		}

		@Override
		protected boolean isRequiredServiceSupported(final BluetoothGatt gatt) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				final BluetoothGattService service = gatt.getService(HR_SERVICE_UUID);
				if (service != null) {
					mHRCharacteristic = service.getCharacteristic(HR_CHARACTERISTIC_UUID);
				}

				//TODO 心率耳机服务
				final BluetoothGattService earphoneService = gatt.getService(HR_SERVICE_BlUETOOTH_EARPHONE_UUID);
				if (earphoneService != null) {
					mHRBtnCharacteristic = earphoneService.getCharacteristic(HR_Button_CHARACTERISTIC_UUID);
					mHRDataCharacteristic = earphoneService.getCharacteristic(HR_DATA_CHARACTERISTIC_UUID);
					mUserSportDataCharacteristic = earphoneService.getCharacteristic(SPORT_DATA_CHARACTERISTIC_UUID);
//					mHRSendCMDCharacteristic = earphoneService.getCharacteristic(HR_SEND_CMD_CHARACTERISTIC_UUID);
//					mHRSendCMDCharacteristic = earphoneService.getCharacteristic(UUID.fromString("87FE8888-A797-45FA-8AD7-2334BAE4CAF1"));
				}
			}
			return mHRCharacteristic != null || mHRBtnCharacteristic != null || mHRDataCharacteristic != null
					|| mUserSportDataCharacteristic != null
//					|| mHRSendCMDCharacteristic != null
					;

		}

//		@Override
//		protected boolean isOptionalServiceSupported(final BluetoothGatt gatt) {
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//				//可选的服务特征值接收
//				final BluetoothGattService service = gatt.getService(HR_SERVICE_UUID);
//				if (service != null) {
//					mHRLocationCharacteristic = service.getCharacteristic(HR_SENSOR_LOCATION_CHARACTERISTIC_UUID);
//				}
//			}
//			return mHRLocationCharacteristic != null;
//		}

		@Override
		public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//			if (mLogSession != null)
//				Logger.a(mLogSession, BodySensorLocationParser.parse(characteristic));

//				if (characteristic.getUuid().equals(HR_SENSOR_LOCATION_CHARACTERISTIC_UUID)) {
//					final String sensorPosition = getBodySensorPosition(characteristic.getValue()[0]);
//					//This will send callback to HRSActivity when HR sensor position on body is found in HR device
//					if (mCallbacks != null) {
//						mCallbacks.onHRSensorPositionFound(sensorPosition);
//					}
//				}
//			}
		}

		@Override
		protected void onDeviceDisconnected() {
//			mHRLocationCharacteristic = null;
			mHRCharacteristic = null;

			//TODO 心率
			mHRBtnCharacteristic = null;
//			mHRSendCMDCharacteristic = null;
			mHRDataCharacteristic = null;

			mUserSportDataCharacteristic = null;

		}

		@Override
		public void onCharacteristicNotified(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				if (characteristic.getUuid().equals(HR_CHARACTERISTIC_UUID)) {
					/**接收到公共心率值 */
					int hrValue;
					if (isHeartRateInUINT16(characteristic.getValue()[0])) {
						hrValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
					} else {
						hrValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
					}
					if (mCallbacks != null) {
						mCallbacks.onHRValueReceived(gatt.getDevice(),hrValue);
					}
				} else if (characteristic.getUuid().equals(HR_DATA_CHARACTERISTIC_UUID)) {
					/**接收到FITMIX定义心率数据 */
					int ifEarphoneOff, signalValue;
					ifEarphoneOff = characteristic.getValue()[2];
					signalValue = characteristic.getValue()[3];
					if (mCallbacks != null) {
						mCallbacks.onSignalValueReceived(gatt.getDevice(),ifEarphoneOff != 1, signalValue);
					}

					if(characteristic.getValue().length<5){
						return;
					}
					int avg_heart_rate,min_heart_rate,max_heart_rate,light_Intensity,current_heart_rate;
					current_heart_rate = unsignedBytesToInt(characteristic.getValue()[1],characteristic.getValue()[0]);
					avg_heart_rate = unsignedBytesToInt(characteristic.getValue()[5],characteristic.getValue()[4]);
					min_heart_rate = unsignedBytesToInt(characteristic.getValue()[7],characteristic.getValue()[6]);
					max_heart_rate = unsignedBytesToInt(characteristic.getValue()[9],characteristic.getValue()[8]);
					light_Intensity = unsignedBytesToInt(characteristic.getValue()[11],characteristic.getValue()[10]);

					if (mCallbacks != null) {
						mCallbacks.onLAVAHRReceive(gatt.getDevice(),current_heart_rate,avg_heart_rate,min_heart_rate,max_heart_rate,light_Intensity);
					}

				} else if (characteristic.getUuid().equals(HR_Button_CHARACTERISTIC_UUID)) {
					/** 接收到按键消息 */
					int pushBtnType, msgType;
//				if(isHeartRateInUINT16(characteristic.getValue()[0])){
//					pushBtnType = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16 , 1);
//				}else{
//					pushBtnType = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8 , 1);
//				}
//				if(isHeartRateInUINT16(characteristic.getValue()[1])){
//					msgType = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16 , 1);
//				}else{
//					msgType = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8 , 1);
//				}
					pushBtnType = characteristic.getValue()[0];
					msgType = characteristic.getValue()[1];
					if (mCallbacks != null) {
						mCallbacks.onPushBtnReceived(gatt.getDevice(),pushBtnType, msgType);
					}
				}else if (characteristic.getUuid().equals(SPORT_DATA_CHARACTERISTIC_UUID)) {
						int sportMode = unsignedBytesToInt(characteristic.getValue()[1], characteristic.getValue()[0]);
						int stepBPM = unsignedBytesToInt(characteristic.getValue()[3], characteristic.getValue()[2]);
						int distance = unsignedBytesToInt(characteristic.getValue()[5], characteristic.getValue()[4]);
						int totalStep = unsignedBytesToInt(characteristic.getValue()[7], characteristic.getValue()[6]);
						int speed = unsignedBytesToInt(characteristic.getValue()[9], characteristic.getValue()[8]);
						int vo2 = unsignedBytesToInt(characteristic.getValue()[11], characteristic.getValue()[10]);
						int calBurnRate = unsignedBytesToInt(characteristic.getValue()[13], characteristic.getValue()[12]);
						int totalCal = unsignedBytesToInt(characteristic.getValue()[15], characteristic.getValue()[14]);
						int maxVo2 = unsignedBytesToInt(characteristic.getValue()[17], characteristic.getValue()[16]);
						if (mCallbacks != null) {
							mCallbacks.onSportDataReceive(gatt.getDevice(),sportMode, stepBPM, distance, totalStep, speed, vo2, calBurnRate, totalCal, maxVo2);
						}
				}
			}
		}

//		@Override
//		protected void onCharacteristicIndicated(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//			super.onCharacteristicIndicated(gatt, characteristic);
//			if (characteristic.getUuid().equals(SPORT_DATA_CHARACTERISTIC_UUID)) {
//				int sportMode = unsignedBytesToInt(characteristic.getValue()[1], characteristic.getValue()[0]);
//				int stepBPM = unsignedBytesToInt(characteristic.getValue()[3], characteristic.getValue()[2]);
//				int distance = unsignedBytesToInt(characteristic.getValue()[5], characteristic.getValue()[4]);
//				int totalStep = unsignedBytesToInt(characteristic.getValue()[7], characteristic.getValue()[6]);
//				int speed = unsignedBytesToInt(characteristic.getValue()[9], characteristic.getValue()[8]);
//				int vo2 = unsignedBytesToInt(characteristic.getValue()[11], characteristic.getValue()[10]);
//				int calBurnRate = unsignedBytesToInt(characteristic.getValue()[13], characteristic.getValue()[12]);
//				int totalCal = unsignedBytesToInt(characteristic.getValue()[15], characteristic.getValue()[14]);
//				int maxVo2 = unsignedBytesToInt(characteristic.getValue()[17], characteristic.getValue()[16]);
//				if (mCallbacks != null) {
//					mCallbacks.onSportDataReceive(sportMode, stepBPM, distance, totalStep, speed, vo2, calBurnRate, totalCal, maxVo2);
//				}
//			}
//		}
	};

//	/**
//	 * This method will decode and return Heart rate sensor position on body
//	 */
//	private String getBodySensorPosition(final byte bodySensorPositionValue) {
//		final String[] locations = getContext().getResources().getStringArray(R.array.hrs_locations);
//		if (bodySensorPositionValue > locations.length)
//			return getContext().getString(R.string.hrs_location_other);
//		return locations[bodySensorPositionValue];
//	}

	/**
	 * This method will check if Heart rate value is in 8 bits or 16 bits
	 */
	private boolean isHeartRateInUINT16(final byte value) {
		return ((value & 0x01) != 0);
	}


	/**
	 * Convert a signed byte to an unsigned int.
	 */
	private int unsignedByteToInt(byte b) {
		return b & 0xFF;
	}

	/**
	 * Convert signed bytes to a 16-bit unsigned int.
	 */
	private int unsignedBytesToInt(byte b0, byte b1) {
		return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
	}
}
