package com.dyq.bletest.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import com.dyq.bletest.Config;
import com.dyq.bletest.R;
import com.dyq.bletest.bean.BleAdapterBean;
import com.dyq.bletest.common.Logger;
import com.dyq.bletest.common.PrefsHelper;
import com.dyq.bletest.common.heartRate.scanner.ExtendedBluetoothDevice;
import com.dyq.bletest.service.HeartRateService;
import com.dyq.bletest.view.adapter.DevicesAdapter;
import com.dyq.bletest.view.widget.SettingDialog;
import com.dyq.bletest.view.widget.SwipeLoadLayout;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends BaseActivity {
    /**
     * 打开系统蓝牙设置界面
     */
    private final static int REQUEST_ENABLE_BLUETOOTH = 21;
    private static final int MSG_UPDATE = 26;
    private static final int DELAYED = 1000;

    private CheckBox cb_start_detect_or_not;
    private SwipeLoadLayout swipeLayout;
    private ListView lv_devices;
    private DevicesAdapter devicesAdapter;
    private ServiceConnection serviceConnection;//心率服务连接
    private HeartRateService heartRateService;//心率服务

    private MyHandler handler;
    public static class MyHandler extends android.os.Handler {

        private WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE:
                    if (mActivity != null && mActivity.get() != null) {
                        mActivity.get().updateProgress();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews(){
//        swipeLayout = (SwipeLoadLayout) findViewById(R.id.swipe_container);
//        swipeLayout.setLoadMoreEnabled(false);
//        swipeLayout.setOnLoadMoreListener(new SwipeLoadLayout.OnLoadMoreListener() {
//
//            @Override
//            public void onLoadMore() {
//
//            }
//        });
        cb_start_detect_or_not = (CheckBox) findViewById(R.id.cb_start_detect_or_not);
        cb_start_detect_or_not.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(!heartRateService.ismIsScanning()){
                        heartRateService.searchHREquipments();
                        cb_start_detect_or_not.setChecked(true);
                        cb_start_detect_or_not.setText(getResources().getString(R.string.stop_detect));
                    }else{
                        cb_start_detect_or_not.setChecked(true);
                        cb_start_detect_or_not.setText(getResources().getString(R.string.stop_detect));
                    }
                }else{
                    if(heartRateService.ismIsScanning()){
                        heartRateService.stopScan();
                        cb_start_detect_or_not.setChecked(false);
                        cb_start_detect_or_not.setText(getResources().getString(R.string.start_detect));
                    }else{
                        cb_start_detect_or_not.setChecked(false);
                        cb_start_detect_or_not.setText(getResources().getString(R.string.start_detect));
                    }
                }
            }
        });

        lv_devices = (ListView) findViewById(R.id.lv_devices);

        devicesAdapter = new DevicesAdapter(this);


//        roundHandler = new RoundHandler(this);

        lv_devices.setAdapter(devicesAdapter);

        lv_devices.setFocusable(false);
        lv_devices.setFocusableInTouchMode(false);

        lv_devices.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                Logger.i(Logger.DEBUG_TAG,"lv_devices,onFoucusChange()");
            }
        });

        handler = new MyHandler(this);

        connectToHeartRateService();

    }

    /**
     * 每秒进行adapter的数据源刷新
     */
    private void updateProgress() {
        if(heartRateService == null)return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(devicesAdapter!=null) {
                    devicesAdapter.setBeanList(heartRateService.getBeansList());
                }
            }
        });

        handler.sendEmptyMessageDelayed(MSG_UPDATE, DELAYED);
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.bt_start_detect:
                if(heartRateService!=null) {
                    heartRateService.searchHREquipments();
                }
                break;
            case R.id.bt_stop_detect:
                if(heartRateService!=null) {
                    heartRateService.stopScan();
                }
                break;

            case R.id.bt_setting:
                if(heartRateService == null)return;
                String name = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).read(Config.BlueToothName,Config.BlueToothDefaultName);
                int signalValueMin = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readInt(Config.BleSignalMin, Config.DefaultMinBleSignal);
                int hrMin = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readInt(Config.BleHRMIN,Config.DEFAULTHRMIN);
                int hrMax = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readInt(Config.BleHRMAX, Config.DEFAULTHRMAX);
                int lightIntensityMin =  PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readInt(Config.BLELIGHTINTENSITYMIN, Config.DEFAULTLIGHTINTENSITYMIN);
                boolean notAutoConnect = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readBoolean(Config.BleNotAutoConnect, Config.DefaultIfNotAutoConnect);


                SettingDialog dialog = SettingDialog.getInstance(name, signalValueMin,
                        hrMin, hrMax, lightIntensityMin, notAutoConnect);

//                SettingDialog dialog = SettingDialog.getInstance(heartRateService.getDeviceName(),heartRateService.getDeviceMinRssi(),
//                        heartRateService.getHrMin(),heartRateService.getHrMax(),
//                        heartRateService.getLightIntensityMin(),heartRateService.isNotAutoConnect());

                dialog.setFilter(new SettingDialog.ConnectFilter() {
//                    @Override
//                    public void setConnectFilter(String deviceName, int minRssi) {
//                        if(heartRateService!=null){
//                            heartRateService.setDeviceMinRssi(minRssi);
//                            heartRateService.setDeviceName(deviceName);
//                            Logger.i(Logger.DEBUG_TAG,"setConnectFilter(),deviceName:"+deviceName+",minRssi:"+minRssi);
//                        }
//                    }
//
//                    @Override
//                    public void setPassFilter(int HrMin, int HrMax, int LightIntensityMin) {
//                        if(heartRateService!=null){
//                            heartRateService.setHrMin(HrMin);
//                            heartRateService.setHrMax(HrMax);
//                            heartRateService.setLightIntensityMin(LightIntensityMin);
//                            Logger.i(Logger.DEBUG_TAG,"setPassFilter(),HrMin:"+HrMin+",HrMax:"+HrMax+",LightIntensityMin:"+LightIntensityMin);
//                        }
//                    }

                    @Override
                    public void setFreshData(String deviceName, int minRssi, int HrMin, int HrMax, int LightIntensityMin,boolean notAutoConnect,boolean restartService) {
                        //TODO SharePreference存储

                        PrefsHelper.with(MainActivity.this,Config.PREFS_USER).writeInt(Config.BleHRMIN,HrMin);
                        PrefsHelper.with(MainActivity.this,Config.PREFS_USER).writeInt(Config.BleHRMAX,HrMax);
                        PrefsHelper.with(MainActivity.this,Config.PREFS_USER).writeInt(Config.BLELIGHTINTENSITYMIN,LightIntensityMin);
                        PrefsHelper.with(MainActivity.this,Config.PREFS_USER).writeBoolean(Config.BleNotAutoConnect,notAutoConnect);
                        //TODO 重新清除，重新连接

                        if(heartRateService!=null) {
                            heartRateService.setHrMin(HrMin);
                            heartRateService.setHrMax(HrMax);
                            heartRateService.setLightIntensityMin(LightIntensityMin);
                            heartRateService.setNotAutoConnect(notAutoConnect);

                            if(restartService) {
                                Logger.i(Logger.DEBUG_TAG, "MainActivity,setFreshData(),restartService***************");
                                PrefsHelper.with(MainActivity.this,Config.PREFS_USER).write(Config.BlueToothName, deviceName);
                                PrefsHelper.with(MainActivity.this,Config.PREFS_USER).writeInt(Config.BleSignalMin, minRssi);

                                heartRateService.setDeviceMinRssi(minRssi);
                                heartRateService.setDeviceName(deviceName);

                                heartRateService.stopScan();
//                                heartRateService.removeAllClickToDisconnectList();
                                heartRateService.restartService();
                                heartRateService.searchHREquipments();

                            }
                        }
                    }
                });
                dialog.show(getFragmentManager(),"SettingDialog");
                break;
        }
    }

    /** 断开连接指定BLE设备
     * @param deviceName
     */
    public void disconnectDevice(String deviceName){
        if(TextUtils.isEmpty(deviceName)){
            return;
        }
        if(heartRateService == null){
            return;
        }
        //
        List<BleAdapterBean> beansList = heartRateService.getBeansList();
        if(beansList == null || beansList.size() == 0)
            return;
        //展示列表
//        for(int i =0 ;i<beansList.size();i++) {
//            BleAdapterBean bean = beansList.get(i);
//            if(bean.getBleMacName().equals(deviceName)){
//                clearBleAdapterBean(bean);
                //断开连接

                heartRateService.addClickToDisconnectDeviceList(deviceName);
                heartRateService.getmBleManager().disconnect(deviceName);

//                if(heartRateService.getmBleManager().disconnect(deviceName))
//                {
//                    heartRateService.addClickToDisconnectDeviceList(deviceName);
//                }
//            }
//        }

    }

    /**
     * 手动断开已连接的设备
     * @param bean
     */
    private void clearBleAdapterBean(BleAdapterBean bean){
        bean.setFlag(Config.DeviceDisconnectedBuffer);
        bean.setBleBleHrValue(0);
        bean.setBleLightIntensity(0);
//        bean.setBleShineMode(-1);
        bean.setBleSignal(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(handler!=null) {
            handler.sendEmptyMessageDelayed(MSG_UPDATE, DELAYED);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.i(Logger.DEBUG_TAG,"MainActivity,onDestroy()");
        disconnectToHeartRateService();
    }


    /**
     * 绑定心率服务
     */
    private void connectToHeartRateService() {
        Intent intent = new Intent(this, HeartRateService.class);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (!name.getClassName().equals(HeartRateService.SERVICE_NAME))
                    return;
                heartRateService = ((HeartRateService.LocalBinder) service).getService();
                if (heartRateService != null) {
                    String blueToothName = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).read(Config.BlueToothName,Config.BlueToothDefaultName);
                    int signalValueMin = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readInt(Config.BleSignalMin, Config.DefaultMinBleSignal);
                    int hrMin = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readInt(Config.BleHRMIN,Config.DEFAULTHRMIN);
                    int hrMax = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readInt(Config.BleHRMAX, Config.DEFAULTHRMAX);
                    int lightIntensityMin =  PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readInt(Config.BLELIGHTINTENSITYMIN, Config.DEFAULTLIGHTINTENSITYMIN);
                    boolean notAutoConnect = PrefsHelper.with(MainActivity.this, Config.PREFS_USER).readBoolean(Config.BleNotAutoConnect, Config.DefaultIfNotAutoConnect);

                    heartRateService.setDeviceName(blueToothName);
                    heartRateService.setDeviceMinRssi(signalValueMin);
                    heartRateService.setHrMin(hrMin);
                    heartRateService.setHrMax(hrMax);
                    heartRateService.setLightIntensityMin(lightIntensityMin);
                    heartRateService.setNotAutoConnect(notAutoConnect);

                    heartRateService.setHeartRateServiceFunction(heartRateServiceFunction);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                heartRateService = null;
            }
        };

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    }


    //region ##################### HeartRateService的回调 #####################

    private HeartRateService.HeartRateServiceFunction heartRateServiceFunction = new HeartRateService.HeartRateServiceFunction() {

//        @Override
//        public void onDeviceConnected(BluetoothDevice device) {}

//        @Override
//        public void onDeviceDisconnected(BluetoothDevice device) {}

//        @Override
//        public void onError() {
//            Logger.i("TT", "HeartRateActivity,onError()");
//        }

//        @Override
//        public void onHRValueReceived(BluetoothDevice device,int hrValue) {
//        }

//        @Override
//        public void onSignalValueReceived(BluetoothDevice device,boolean deviceOff, int signalValue) {}

//        @Override
//        public void onLAVAHRReceive(BluetoothDevice device,int current_hr, int avg_hr, int min_hr, int max_hr) {}

//        @Override
//        public void onSportDataReceive(BluetoothDevice device, int sportMode, int stepBPM, int distance, int totalStep, int speed, int vo2, int calBurnRate, int totalCal, int maxVo2) {}
    };


    //endregion ##################### HeartRateService的回调 #####################


    /**
     * 解绑心率服务
     */
    private void disconnectToHeartRateService() {
        heartRateServiceFunction = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (heartRateService != null) {
                heartRateService.setHeartRateServiceFunction(null);
            }
        }
        if (serviceConnection != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                unbindService(serviceConnection);
            }
        }
        serviceConnection = null;
    }


    /**
     * 从已展示列表中删除某项
     * @param deviceMacAddress
     */
    public void removeDeviceFromList(String deviceMacAddress){
        if(heartRateService!=null) {
            if(TextUtils.isEmpty(deviceMacAddress)){
                return;
            }
            List<BleAdapterBean> beansList = heartRateService.getBeansList();//展示的列表去除该item
            if(beansList != null && beansList.size() != 0){
                for(int i = 0;i<beansList.size();i++) {
                    if (beansList.get(i).getBleMacName().equals(deviceMacAddress)) {
                        beansList.remove(i);
                    }
                }
            }

            List<ExtendedBluetoothDevice> searchedList = heartRateService.getmListValues();//搜索的列表去除该item
            if(searchedList != null && searchedList.size() != 0){
                for(int i = 0;i<searchedList.size();i++) {
                    if(searchedList.get(i).device.getAddress().equals(deviceMacAddress)){
                        searchedList.remove(i);
                        break;
                    }
                }
            }

        }
    }



}
