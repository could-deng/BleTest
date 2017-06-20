package com.dyq.bletest.view;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.dyq.bletest.Config;
import com.dyq.bletest.R;
import com.dyq.bletest.bean.BleAdapterBean;
import com.dyq.bletest.bean.ChartBean;
import com.dyq.bletest.bean.HrChartBean;
import com.dyq.bletest.common.Logger;
import com.dyq.bletest.common.PrefsHelper;
import com.dyq.bletest.common.heartRate.BleManager;
import com.dyq.bletest.common.heartRate.HRSManager;
import com.dyq.bletest.common.heartRate.scanner.ExtendedBluetoothDevice;
import com.dyq.bletest.common.heartRate.scanner.ScannerFragment;
import com.dyq.bletest.service.HeartRateService;
import com.dyq.bletest.view.adapter.ChartDeviceAdapter;
import com.dyq.bletest.view.widget.CompareHRChart;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

/**
 * Created by yuanqiang on 2017/6/16.
 */

public class ChartActivity extends BaseActivity implements ScannerFragment.OnDeviceSelectedListener{

    private ChartDeviceAdapter devicesAdapter;
    private ListView lv_chart_devices;
    private CompareHRChart hr_compare_chart;

    private HeartRateService heartRateService;
    private ServiceConnection serviceConnection;
    private static final int MSG_UPDATE = 26;
    private static final int DELAYED = 1000;
    private ChartActivity.MyHandler handler;
    @Override
    public void onDeviceSelected(ExtendedBluetoothDevice device) {
        Logger.i(Logger.DEBUG_TAG, device.device.getAddress());
        if(heartRateService!=null){
            heartRateService.manualConnect(device);
        }
    }

    @Override
    public void onDialogCanceled() {

    }

    public static class MyHandler extends android.os.Handler {

        private WeakReference<ChartActivity> mActivity;

        public MyHandler(ChartActivity activity) {
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



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        init();

    }

    private void init(){
        lv_chart_devices = (ListView) findViewById(R.id.lv_chart_devices);
        hr_compare_chart = (CompareHRChart) findViewById(R.id.hr_compare_chart);
        devicesAdapter = new ChartDeviceAdapter(this);
        lv_chart_devices.setAdapter(devicesAdapter);
        handler = new MyHandler(this);
        connectToHeartRateService();
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.bt_add_devices:
                showDeviceScanningDialog();
                break;
        }
    }


    /**
     * Shows the scanner fragment.
     */
    private void showDeviceScanningDialog() {
        final ScannerFragment dialog = ScannerFragment.getInstance(HRSManager.HR_SERVICE_UUID);
        dialog.show(getSupportFragmentManager(), "scan_fragment");
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
                    String blueToothName = PrefsHelper.with(ChartActivity.this, Config.PREFS_USER).read(Config.BlueToothName,Config.BlueToothDefaultName);
                    int signalValueMin = Config.DefaultMinBleSignal;
                    int hrMin = Config.DEFAULTHRMIN;
                    int hrMax = Config.DEFAULTHRMAX;
                    int lightIntensityMin =  Config.DEFAULTLIGHTINTENSITYMIN;
                    boolean notAutoConnect = false;

                    heartRateService.setServeForChartActivity(true);
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
    /**
     * 解绑心率服务
     */
    private void disconnectToHeartRateService() {
        heartRateServiceFunction = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (heartRateService != null) {
                heartRateService.stopSecondlyMethod();
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

    private HeartRateService.HeartRateServiceFunction heartRateServiceFunction = new HeartRateService.HeartRateServiceFunction() {
        @Override
        public void reDrawHeartRateData() {
            if(heartRateService == null || heartRateService.getBeansList().size() == 0){
                return;
            }
            List<BleAdapterBean> ll = heartRateService.getBeansList();
            BleAdapterBean adapterBean = ll.get(0);
            hr_compare_chart.addValue(adapterBean.getBleMacName(),new ChartBean(adapterBean.getBleBleHrValue(),heartRateService.getRefreshTime()),true);
        }
    };

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
        disconnectToHeartRateService();
    }


}
