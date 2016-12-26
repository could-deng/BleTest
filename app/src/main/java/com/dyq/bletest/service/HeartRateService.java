package com.dyq.bletest.service;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.dyq.bletest.Config;
import com.dyq.bletest.bean.BleAdapterBean;
import com.dyq.bletest.common.Logger;
import com.dyq.bletest.common.PrefsHelper;
import com.dyq.bletest.common.ServiceAliveHelper;
import com.dyq.bletest.common.WeakHandler;
import com.dyq.bletest.common.heartRate.BleManager;
import com.dyq.bletest.common.heartRate.BleManagerCallbacks;
import com.dyq.bletest.common.heartRate.HRSManager;
import com.dyq.bletest.common.heartRate.HRSManagerCallbacks;
import com.dyq.bletest.common.heartRate.scanner.ExtendedBluetoothDevice;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;


/**
 * 心率服务
 */
public class HeartRateService extends Service {
    /**
     * HeartRateService 名称
     */
    public static String SERVICE_NAME = HeartRateService.class.getName();

    private BleManager<? extends BleManagerCallbacks> mBleManager;
//    private List<BluetoothDevice> deviceList;

    private ArrayList<ExtendedBluetoothDevice> mListValues;//搜索到要去连接的的BLE设备,（过滤掉在beansList中未连接的设备)
    private ArrayList<ExtendedBluetoothDevice> allSearchedDevice; //搜索到的全部BLE设备
    private ArrayList<BleAdapterBean> beansList;//已连接和(搜索到，未连接)的BLE设备Bean类

    private ArrayList<String> mClickToDisconnectDeviceList;//点击去断开连接的BLE设备


    //region ##################### 过滤条件参数 #####################

    //自动连接的条件
    private int deviceMinRssi ;//= 0
    private String deviceName = "";//="LAVA H10";//"Shine H10"

    //过滤条件
    private int hrMin ;//= 0
    private int hrMax ;//= 0
    private int LightIntensityMin;
    private boolean notAutoConnect;//= false

    public void setDeviceMinRssi(int deviceMinRssi) {
        this.deviceMinRssi = deviceMinRssi;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setHrMin(int hrMin) {
        this.hrMin = hrMin;
    }

    public void setHrMax(int hrMax) {
        this.hrMax = hrMax;
    }

    public void setLightIntensityMin(int lightIntensityMin) {
        LightIntensityMin = lightIntensityMin;
    }

    public void setNotAutoConnect(boolean notAutoConnect) {
        this.notAutoConnect = notAutoConnect;
    }


    //endregion ##################### 过滤条件参数 #####################

    //region ##################### BLE搜索并连接 #####################

    private static final int MSG_UPDATE = 26;
    private int DELAYED = 1000;

    private static final int MSG_REFRESH_SEARCHED_LIST = 25;//刷新搜索到的设备列表
    private int REFRESH_LIST_DELAY = 5000;//刷新搜索到的设备列表延迟


    //自动搜索
    private boolean mIsScanning = false;
    //    private boolean isConnecting;//正在连接

    public boolean ismIsScanning() {
        return mIsScanning;
    }

    private MyHandler myHandler;
    /**
     * 自定义handler
     */
    protected class MyHandler extends WeakHandler {
        public MyHandler(Service heartRateService) {
            super(heartRateService);
        }

        public void handleMessage(Message msg) {
            HeartRateService service = (HeartRateService) getRefercence();
            if (service == null)
                return;
            switch (msg.what) {
                case MSG_UPDATE://每秒刷新一次运动数据
                    updateSearchingProgress();
                    break;

                case MSG_REFRESH_SEARCHED_LIST://刷新搜索到的设备列表
                    updateSearchedDevice();
                    break;
            }
        }
    }
    /**
     * 获取Handler,保证不为null
     */
    private MyHandler getMyHandler() {
        if (myHandler == null) {
            myHandler = new MyHandler(this);
        }
        return myHandler;
    }

    /**
     * 每秒都发送搜索设备命令，同时如果搜索到设备则进行连接操作
     */
    private void updateSearchingProgress(){
        Logger.i(Logger.DEBUG_TAG, "updateSearchingProgress(),mListValues.size():" + mListValues.size());
        if(mListValues.size()>0){
                connectDevice();
        }
        getMyHandler().sendEmptyMessageDelayed(MSG_UPDATE, DELAYED);
    }

    /**
     * 从beansList列表中筛选Config.DeviceSearchedWithoutConnected的设备，去除搜索不到设备
     */
    private void updateSearchedDevice(){
        if(allSearchedDevice == null || getBeansList()==null){
            return;
        }
        Logger.i(Logger.DEBUG_TAG,"updateSearchedDevice(),allSearchedDevice.size():"+allSearchedDevice.size());
        ArrayList<BleAdapterBean> toRemoveList = new ArrayList<>();
        if(allSearchedDevice == null || allSearchedDevice.size() == 0){
            ArrayList<BleAdapterBean> aa = new ArrayList<>();
            for(int i = 0;i<beansList.size();i++){
                BleAdapterBean bean = beansList.get(i);
                if(bean.getFlag() != Config.DeviceSearchedWithoutConnected){
                    continue;
                }
                aa.add(bean);
            }
            if(beansList.size()!=0 && aa.size() != 0 ){
                for(int i =0 ;i<aa.size() ;i++) {
                    beansList.remove(aa.get(i));
                }
            }
            Logger.i(Logger.DEBUG_TAG,"removeAllClickToDisconnectList");
            removeAllClickToDisconnectList();
            getMyHandler().sendEmptyMessageDelayed(MSG_REFRESH_SEARCHED_LIST, REFRESH_LIST_DELAY);
            return;
        }
        for(int i = 0; i< beansList.size();i++){
            BleAdapterBean bean = beansList.get(i);
            if(bean.getFlag() != Config.DeviceSearchedWithoutConnected){//只有未连接才做接下来的操作
                continue;
            }
            boolean haveSearched = false;
            for(int j = 0 ; j<allSearchedDevice.size() ;j++){
                ExtendedBluetoothDevice ddd = allSearchedDevice.get(j);
                if(ddd.device == null){
                    continue;
                }
                if(ddd.device.getAddress().equals(bean.getBleMacName())){
                    bean.setBleSignal(ddd.rssi);
                    haveSearched = true;
                    break;
                }
            }
            if(!haveSearched){
                toRemoveList.add(bean);
            }
        }
        for(int i = 0;i<toRemoveList.size();i++){
            BleAdapterBean bb = toRemoveList.get(i);
            beansList.remove(bb);
            Logger.i(Logger.DEBUG_TAG,"beansList.remove(bb):"+bb.getBleMacName());
            for(String name:getClickToDisconnectDeviceList()){
                if(bb.getBleMacName().equals(name)){
                    getClickToDisconnectDeviceList().remove(name);
                    Logger.i(Logger.DEBUG_TAG, "getClickToDisconnectDeviceList.size():"+ getClickToDisconnectDeviceList().size() +",removeItem:"+name);
                    break;
                }
            }
        }

        allSearchedDevice.clear();
        getMyHandler().sendEmptyMessageDelayed(MSG_REFRESH_SEARCHED_LIST, REFRESH_LIST_DELAY);
    }

    /**
     * 当设备断开后，重新启动搜索searchDeviceList任务，因为避免一断开设备，还没有搜索到设备的时候，马上执行UpdateSearchedDevice()，导致列表去除该项，又增加。
     */
    public void reStartUpdateSearchedDeviceTask(){
        getMyHandler().removeMessages(MSG_REFRESH_SEARCHED_LIST);
        getMyHandler().sendEmptyMessageDelayed(MSG_REFRESH_SEARCHED_LIST, REFRESH_LIST_DELAY);
    }
    /**
     * 自动搜索并连接HR设备
     */
    public void searchHREquipments(){
        if(!mIsScanning) {
            if (isBLEEnabled()) {//蓝牙开启了
                mIsScanning = true;
                getMyHandler().sendEmptyMessageDelayed(MSG_UPDATE, DELAYED);
                getMyHandler().sendEmptyMessageDelayed(MSG_REFRESH_SEARCHED_LIST , REFRESH_LIST_DELAY);

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
                final ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000).setUseHardwareBatchingIfSupported(false).build();
                final List<ScanFilter> filters = new ArrayList<>();
                filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HRSManager.HR_SERVICE_UUID)).build());
                scanner.startScan(filters, settings, scanCallback);
            } else {//未开启蓝牙
                Toast.makeText(HeartRateService.this,"请打开蓝牙",Toast.LENGTH_SHORT).show();
                requestBlueTooth();
            }
        }
    }


    /**
     * 停止搜索（一直搜索到）
     */
    public void stopScan(){
        Logger.i(Logger.DEBUG_TAG,"HeartRateService,stopScan()");
        if(mIsScanning) {
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(scanCallback);
            mIsScanning = false;
        }
    }

    /**
     * 设备搜索回调
     */
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {

        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            for (final ScanResult result : results) {
                String name = PrefsHelper.with(HeartRateService.this, Config.PREFS_USER).read(Config.BLEFILTERNAME,Config.BleName);
                if(!result.getDevice().getName().contains(name)){//过滤掉不是H10设备的
                    continue;
                }
                final ExtendedBluetoothDevice device = findDevice(result);//与mListValues进行去重过滤
                if (device == null) {
                    ExtendedBluetoothDevice extendedBluetoothDevice = new ExtendedBluetoothDevice(result);
                    boolean haveConnectSameDevice = false;
                    if(beansList!=null) {
                        for (int i = 0; i < beansList.size(); i++) {//过滤掉已连接的设备
                            BleAdapterBean bean = beansList.get(i);
                            if (extendedBluetoothDevice.device.equals(bean.getDevice())) {
                                if ((bean.getFlag() == Config.DeviceConnectAndMate) || (bean.getFlag() == Config.DeviceConnectNotMate)) {
                                    haveConnectSameDevice = true;
                                    break;
                                }
                            }
                        }
                    }
                    if(!haveConnectSameDevice) {//第一重过滤没有过滤成功的话进行第二重过滤
                        Logger.i(Logger.DEBUG_TAG,"getClickToDisconnectDeviceList():"+getClickToDisconnectDeviceList().size());
                        for(int i =0;i<getClickToDisconnectDeviceList().size() ;i++){
                            Logger.i(Logger.DEBUG_TAG, "ClickToDisconnectDeviceListTemp:" + getClickToDisconnectDeviceList().get(i));
                        }
                        for (String deviceName : getClickToDisconnectDeviceList()) {//过滤掉手动断开的设备
                            if (extendedBluetoothDevice.device.getAddress().equals(deviceName)) {
                                haveConnectSameDevice = true;
                                break;
                            }
                        }
                    }

                    if(!haveConnectSameDevice) {
                        boolean haveSearchedDefore = false;
                        if(allSearchedDevice!=null) {
                            for (ExtendedBluetoothDevice dd : allSearchedDevice) {
                                if (dd.device.getAddress().equals(extendedBluetoothDevice.device.getAddress())) {
                                    haveSearchedDefore = true;
                                    break;
                                }
                            }
                        }

                        if(!haveSearchedDefore && mListValues!=null){
                            mListValues.add(extendedBluetoothDevice);
                        }
                    }
                } else {//mListValues数据刷新
                    device.name = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
                    device.rssi = result.getRssi();
                }



                final ExtendedBluetoothDevice searchedDevice = findDeviceFromSearchedList(result);//与allSearchedDevice进行去重过滤
                if(searchedDevice == null && allSearchedDevice!=null){
                    ExtendedBluetoothDevice extendedBluetoothDevice = new ExtendedBluetoothDevice(result);
                    allSearchedDevice.add(extendedBluetoothDevice);

                }else if(searchedDevice != null){
                    searchedDevice.name = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
                    searchedDevice.rssi = result.getRssi();
                }
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {

        }
    };

    /**
     * 与将要连接的BLE设备集合进行去重过滤
     * @param result
     * @return
     */
    private ExtendedBluetoothDevice findDevice(final ScanResult result) {
        if(mListValues == null){
            return null;
        }
        for (final ExtendedBluetoothDevice device : mListValues)
            if (device.matches(result))
                return device;
        return null;
    }

    /**
     * 与搜索到的全部BLE设备集合进行去重过滤
     * @param result
     * @return
     */
    private ExtendedBluetoothDevice findDeviceFromSearchedList(final ScanResult result){
        if(allSearchedDevice == null)
            return null;
        for (final ExtendedBluetoothDevice device : allSearchedDevice) {
            if(device == null){
                continue;
            }
            if (device.matches(result))
                return device;
        }
        return null;
    }

    /**
     * 根据搜索获取的蓝牙设备集，连接设备
     */
    private void connectDevice(){
        if(mListValues.size() <= 0){
            return;
        }
        ExtendedBluetoothDevice extendedBlueToothDevice = mListValues.get(0);
        BluetoothDevice bluetoothDevice = extendedBlueToothDevice.device;
        for(int i = 0; i<beansList.size();i++){
            if(bluetoothDevice.getAddress().equals(beansList.get(i).getBleMacName())){
                mListValues.remove(0);
                return;
            }
        }
        if (getmBleManager() != null) {
            //判断是否满足自动连接的条件，选择性自动连接
            if(!notAutoConnect) {
                if(getConnectedDeivceNum()<=Config.DeviceConnectNumMax) {
                    if ((!TextUtils.isEmpty(deviceName) && bluetoothDevice.getName().contains(deviceName)) && extendedBlueToothDevice.rssi > deviceMinRssi) {
                        beansList.add(new BleAdapterBean(bluetoothDevice, getLeastLightMode(), extendedBlueToothDevice.rssi, Config.DeviceSearchedWithoutConnected));
                        getmBleManager().connect(bluetoothDevice);
                    } else {
                        beansList.add(new BleAdapterBean(bluetoothDevice, -1, extendedBlueToothDevice.rssi, Config.DeviceSearchedWithoutConnected));
                        mListValues.remove(0);
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"BLE连接数达到上限",Toast.LENGTH_SHORT).show();
                }
            }else{
                beansList.add(new BleAdapterBean(bluetoothDevice, -1, extendedBlueToothDevice.rssi, Config.DeviceSearchedWithoutConnected));
                mListValues.remove(0);
            }
        }
    }

    /**
     * 获取已连接的BLE设备数
     * @return
     */
    private int getConnectedDeivceNum(){
        ArrayList<BleAdapterBean> beanList = getBeansList();
        int num = 0;
        for(int i = 0 ;i<beanList.size();i++){
            BleAdapterBean bean = beanList.get(i);
            if(bean.getFlag() == Config.DeviceConnectAndMate || bean.getFlag() == Config.DeviceConnectNotMate){
                num++;
            }
        }
        return num;
    }
    private int getLeastLightMode(){
        if(beansList.size() == 0){
            return 5;
        }

        int[] lightModeData = new int[3];
        for(int i =0;i<beansList.size();i++){
            BleAdapterBean bb = beansList.get(i);
            switch (bb.getBleShineMode()){
                case 1:
                    lightModeData[0]++;
                    break;
                case 2:
                    lightModeData[1]++;
                    break;
                case 5:
                    lightModeData[2]++;
                    break;
            }
        }
        int ll = 1;
        int leastIndex = lightModeData[0];//默认最少的灯光模式的数量
        for(int i = 1;i< 3 ;i++){
            if(leastIndex > lightModeData[i]){
                leastIndex = lightModeData[i];
                switch (i){
                    case 0:
                        ll = 1;
                        break;
                    case 1:
                        ll = 2;
                        break;
                    case 2:
                        ll = 5;
                        break;
                }
            }
        }
        return ll;

    }
    //endregion ##################### BLE搜索并连接 #####################



    //region ================================== 服务的接口回调 ==================================

    public interface HeartRateServiceFunction{
//        void onDeviceConnected(BluetoothDevice device);
//        void onDeviceDisconnected(BluetoothDevice device);
//        void onError();
        /** 接收到心率数值 */
//        void onHRValueReceived(BluetoothDevice device,int hrValue);
//        /** 接收到心率设备信号值
//         * @param deviceOff
//         * @param signalValue
//         */
//        void onSignalValueReceived(BluetoothDevice device,boolean deviceOff, int signalValue);

//        void onLAVAHRReceive(BluetoothDevice device,int current_heart_rate,int avg_hr, int min_hr, int max_hr);

//        void onSportDataReceive(BluetoothDevice device,int sportMode, int stepBPM, int distance, int totalStep, int speed, int vo2, int calBurnRate, int totalCal, int maxVo2);
    }

    private HeartRateServiceFunction heartRateServiceFunction;


    public void setHeartRateServiceFunction(HeartRateServiceFunction heartRateServiceFunction) {
        this.heartRateServiceFunction = heartRateServiceFunction;
    }
    //endregion ================================== 服务的接口回调 ==================================



    //region ================================== Service生命周期相关 ==================================

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public HeartRateService getService() {
            return HeartRateService.this;
        }
    }

    @Override
    public void onCreate() {
        Logger.i(Logger.DEBUG_TAG, "HeartRateService --- > onCreate()");
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            inits();
        }
    }


    public void inits() {
        mListValues = new ArrayList<>();
        beansList = new ArrayList<>();
        allSearchedDevice = new ArrayList<>();//搜索到的全部BLE设备
        mClickToDisconnectDeviceList = new ArrayList<>();

        /*
		 * We use the managers using a singleton pattern. It's not recommended for the Android, because the singleton instance remains after Activity has been
		 * destroyed but it's simple and is used only for this demo purpose. In final application Managers should be created as a non-static objects in
		 * Services. The Service should implement ManagerCallbacks interface. The application Activity may communicate with such Service using binding,
		 * broadcast listeners, local broadcast listeners (see support.v4 library), or messages. See the Proximity profile for Service approach.
		 */
        mBleManager = initializeManager();
    }

    public void addClickToDisconnectDeviceList(String macAddress){
        ArrayList<String> aa = getClickToDisconnectDeviceList();
        boolean haveExist = false;
        for(int i =0 ;i<aa.size();i++){
            if(aa.get(i).equals(macAddress)){
                haveExist = true;
                break;
            }
        }
        if(!haveExist){
            getClickToDisconnectDeviceList().add(macAddress);
        }
    }
    private ArrayList<String> getClickToDisconnectDeviceList(){
        if(mClickToDisconnectDeviceList == null){
            mClickToDisconnectDeviceList = new ArrayList<>();
        }
        return mClickToDisconnectDeviceList;
    }

    public void removeAllClickToDisconnectList(){
        if(getClickToDisconnectDeviceList().size()>0) {
            getClickToDisconnectDeviceList().clear();
        }
    }

    public ArrayList<BleAdapterBean> getBeansList() {
        if(beansList == null){
            beansList = new ArrayList<>();
        }
        return beansList;
    }

    public ArrayList<ExtendedBluetoothDevice> getmListValues() {
        if(mListValues == null){
            mListValues = new ArrayList<>();
        }
        return mListValues;
    }

    protected BleManager<HRSManagerCallbacks> initializeManager() {
        Logger.i(Logger.DEBUG_TAG,"initializeManager()");

        HRSManager manager = HRSManager.getInstance(this);

        manager.setGattCallbacks(getDefaultCallBack());
        return manager;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
    }

    @Override
    public void onDestroy() {
        Logger.i(Logger.DEBUG_TAG, "HeartRateService --- > onDestroy()");
        stopScan();
        getMyHandler().removeCallbacksAndMessages(null);
        release();
        super.onDestroy();
    }

    public void restartService(){
        //断开连接
        if(mBleManager!=null) {
            mBleManager.disconnect();

            mListValues.clear();
            beansList.clear();
            allSearchedDevice.clear();
            removeAllClickToDisconnectList();
        }
    }
    /**
     * 释放资源
     */
    public void release() {
        //断开连接
        if(mBleManager!=null) {
            mBleManager.disconnect();
            mBleManager.close();
            mBleManager.setGattCallbacks(null);
            mBleManager = null;
        }

//        deviceList = null;
        defaultCallBack = null;
        heartRateServiceFunction = null;
//        batteryValue = 0;


        mListValues = null;
        beansList = null;
        allSearchedDevice = null;

        //过滤条件
        deviceMinRssi = 0;
        deviceName= "";
        hrMin = 0;
        hrMax = 0;
        LightIntensityMin = 0;

    }


    public BleManager<? extends BleManagerCallbacks> getmBleManager() {
        return mBleManager;
    }

//    public List<BluetoothDevice> getDevice() {
//        return deviceList;
//    }
//
//    public void setDevice(List<BluetoothDevice> device) {
//        this.deviceList = device;
//    }
//
//    public void addDevice(BluetoothDevice device){
//        this.deviceList.add(device);
//    }

    //endregion ================================== Service生命周期相关 ==================================


    //region ##################### HRSManagerCallbacks的相关回调 #####################
    /**
     * 默认的回调，当不在心率的相关activity时，使得心率断开时销毁该HeartRateService
     */
    HRSManagerCallbacks defaultCallBack = new HRSManagerCallbacks() {
        @Override
        public void onHRSensorPositionFound(String position) {
        }

        @Override
        public void onHRValueReceived(BluetoothDevice device,int value) {
        }

        @Override
        public void onDeviceConnected(BluetoothDevice device){
            Logger.i(Logger.DEBUG_TAG,device.getAddress()+",onDeviceConnected");

            //扫描结果列表去除掉连接的设备名称
            for(int i =0 ;i<mListValues.size();i++){
                if(mListValues.get(i).device.getAddress().equals(device.getAddress())){
                    mListValues.remove(i);
                    break;
                }
            }
            int index = 0;
            boolean setFlagSuccessful = false;
            for(int i =0 ;i<beansList.size();i++) {
                if (beansList.get(i).getBleMacName().equals(device.getAddress())){
                    index = i;
                    beansList.get(i).setFlag(Config.DeviceConnectNotMate);//一连接就设为已连接但不符合标准Flag
                    setFlagSuccessful =  true;
                    break;
                }
            }

            if(!setFlagSuccessful){//如果beansList列表中没有该项，增加该项
                BleAdapterBean bb = new BleAdapterBean(device, getLeastLightMode(), 0,Config.DeviceConnectNotMate);
                beansList.add(bb);
                getmBleManager().sendHRCmd(device.getAddress(), bb.getBleShineMode());
            }
            else{
                getmBleManager().sendHRCmd(device.getAddress(),beansList.get(index).getBleShineMode());
            }

//            if(isConnecting){
//                isConnecting = false;
//            }
        }

        @Override
        public void onDeviceDisconnecting(String deviceName) {
            for(int i =0 ;i<beansList.size();i++) {
                BleAdapterBean bean = beansList.get(i);
                if (bean == null || TextUtils.isEmpty(bean.getBleMacName())) continue;
                if (bean.getBleMacName().equals(deviceName)) {
                    bean.setFlag(Config.DeviceDisconnectedBuffer);
                    bean.setBleBleHrValue(0);
                    bean.setBleLightIntensity(0);
//        bean.setBleShineMode(-1);
                    bean.setBleSignal(0);
                }
            }

        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device){
            Logger.i(Logger.DEBUG_TAG, device.getAddress()+", onDeviceDisconnected");
            //已连接设备集合过滤
            for(int i =0 ;i<beansList.size();i++) {
                BleAdapterBean bean = beansList.get(i);
                if(bean == null || TextUtils.isEmpty(bean.getBleMacName())) continue;
                if (bean.getBleMacName().equals(device.getAddress())){
                    getBeansList().get(i).setFlag(Config.DeviceSearchedWithoutConnected);//自动监听到断开的
                    reStartUpdateSearchedDeviceTask();
                }
            }

        }

        @Override
        public void onLinklossOccur() {}

        @Override
        public void onServicesDiscovered(boolean optionalServicesFound) {}

        @Override
        public void onDeviceReady() {  }

        @Override
        public void onBondingRequired() { }

        @Override
        public void onBonded() { }

        @Override
        public void onError(String message, int errorCode) {
            Logger.i(Logger.DEBUG_TAG,"HeartRateService onError ------> message:"+message+",errorCode:" + errorCode);
        }

        @Override
        public void onDeviceNotSupported() {
//            Logger.i(Logger.DEBUG_TAG, "onDeviceNotSupported()!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }

        @Override
        public void onSignalValueReceived(BluetoothDevice device,boolean ifDrop, int signalValue) {
            Logger.i(Logger.DEBUG_TAG,device.getAddress()+",onSignalValueReceived()，ifDrop="+(ifDrop?"1":"0")+"，signalValue:"+signalValue);
            int index = -1;
            for(int i=0;i<beansList.size();i++){
                BleAdapterBean bean = beansList.get(i);
                if(bean.getDevice().getAddress().equals(device.getAddress())){
                    index = i;
                    bean.setBleSignal(signalValue);

                }
            }
            /**
             * TODO 不断发送灯光模式改为五秒延迟设置灯光模式
             */
            if(index != -1){
                getmBleManager().sendHRCmd(device.getAddress(),beansList.get(index).getBleShineMode());
            }

        }

        @Override
        public void onPushBtnReceived(BluetoothDevice device,int btnType, int msgType) {
//            Logger.i(Logger.DEBUG_TAG, "HeartRateService,"+device.getAddress()+",onPushBtnReceived(),btnType=" + btnType + "，msgType:" + msgType);
        }

        @Override
        public void onLAVAHRReceive(BluetoothDevice device,int current_hr,int avg_hr, int min_hr, int max_hr,int lightIntensity) {
            Logger.i(Logger.DEBUG_TAG, device.getAddress()+",onLAVAHRReceive(),current_hr:"+current_hr+",lightIntensity="+lightIntensity);
//                    +",avg_hr=" + avg_hr + "，min_hr:" + min_hr + "，max_hr:" + max_hr);
            for(int i=0;i<beansList.size();i++){
                BleAdapterBean bean = beansList.get(i);
                if(bean.getDevice().getAddress().equals(device.getAddress())){
                    if(bean.getFlag() == Config.DeviceDisconnectedBuffer){
                        continue;
                    }
                    bean.setBleBleHrValue(current_hr);
                    bean.setBleLightIntensity((lightIntensity*100)/65535);//除以0xffff,百分比

                    if(bean.getBleLightIntensity() >= LightIntensityMin) {//满足光强度条件
                        if (bean.getBleBleHrValue() >= hrMin && bean.getBleBleHrValue() <= hrMax) {//不满足HR条件
                            bean.setFlag(Config.DeviceConnectAndMate);
                        } else {
                            bean.setFlag(Config.DeviceConnectNotMate);
                        }
                    }else{//不满足光强度条件
                        bean.setFlag(Config.DeviceConnectNotMate);
                    }
                }
            }
        }

        @Override
        public void onSportDataReceive(BluetoothDevice device,int sportMode, int stepBPM, int distance, int totalStep, int speed, int vo2, int calBurnRate, int totalCal, int maxVo2) {
//            Logger.i(Logger.DEBUG_TAG, "HeartRateService,"+device.getAddress()+",onSportDataReceive(),sportMode=" + sportMode + "，stepBPM:" + stepBPM
//                    + "，distance:" + distance + "，totalStep:" + totalStep + "，speed:" + speed + "，vo2:" + vo2
//                    + "，calBurnRate:" + calBurnRate + "，totalCal:" + totalCal + "，maxVo2:" + maxVo2);
        }
    };

    public HRSManagerCallbacks getDefaultCallBack() {
        return defaultCallBack;
    }

    //endregion ##################### HRSManagerCallbacks的相关回调 #####################



    //region ================================== Service保活相关 ==================================

    private PowerManager.WakeLock mCPUWakeLock;//CPU锁,防止CPU休眠

    /**
     * 获取WakeLock
     */
    private PowerManager.WakeLock getCPUWakeLock() {
        if (mCPUWakeLock == null) {
            PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mCPUWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "RunService_CPU_Lock");//在部分手机,熄屏后加速传感器会停止
        }
        return mCPUWakeLock;
    }

    /**
     * 开启CPU锁定
     */
    private void startCPULock() {
        getCPUWakeLock().acquire();
    }

    /**
     * 释放CPU锁定
     */
    private void releaseCPULock() {
        if (mCPUWakeLock != null) {
            getCPUWakeLock().release();
        }
        mCPUWakeLock = null;
    }

    /**
     * 开启Service存活保护
     */
    private void startKeepAlive() {
        ServiceAliveHelper.getInstance(this).startKeep();
    }

    /**
     * 停止Service存活保护
     */
    private void stopKeepAlive() {
        ServiceAliveHelper.getInstance(this).stopKeep();
    }

    //endregion ================================== Service保活相关 ==================================

    /**
     * 是否允许蓝牙4.0权限
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isBLEEnabled() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    /**
     * 请求蓝牙权限，去启动蓝牙
     */
    private void requestBlueTooth() {
//        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
    }

    private UUID getFilterUUID() {
        return HRSManager.HR_SERVICE_UUID;
    }


}
