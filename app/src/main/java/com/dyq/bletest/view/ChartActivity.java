package com.dyq.bletest.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;
import com.dyq.bletest.Config;
import com.dyq.bletest.MixApp;
import com.dyq.bletest.R;
import com.dyq.bletest.bean.BleAdapterBean;
import com.dyq.bletest.bean.ChartBean;
import com.dyq.bletest.common.FileUtils;
import com.dyq.bletest.common.Logger;
import com.dyq.bletest.common.PrefsHelper;
import com.dyq.bletest.common.heartRate.HRSManager;
import com.dyq.bletest.common.heartRate.scanner.ExtendedBluetoothDevice;
import com.dyq.bletest.common.heartRate.scanner.ScannerFragment;
import com.dyq.bletest.model.database.HrInfo;
import com.dyq.bletest.model.database.HrInfoDao;
import com.dyq.bletest.model.database.hrInfoOperatorHelper;
import com.dyq.bletest.service.HeartRateService;
import com.dyq.bletest.view.adapter.ChartDeviceAdapter;
import com.dyq.bletest.view.widget.CompareHRChart;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import de.greenrobot.dao.async.AsyncOperation;
import de.greenrobot.dao.async.AsyncOperationListener;
import de.greenrobot.dao.query.QueryBuilder;

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
            case R.id.bt_clear_devices:
                disconnectAllDevice();
                break;
            case R.id.bt_start_chart_draw:
                if(heartRateService!=null && heartRateService.getBeansList().size()>0){
                    heartRateService.startDrawChart();
                }else{
                    Logger.e(Logger.DEBUG_TAG,"ChartActivity--->heartRateService.getBeansList.size()!>0");
                }
                break;
            case R.id.bt_db_backup:
                backupDatabase();
                break;

            case R.id.bt_write_file:
                String log_start_time = String.valueOf(heartRateService.getStartRecordTime());
                if(heartRateService.getBeansList() == null && heartRateService.getBeansList().size() == 0){
                    return;
                }
                writeSDcardFromDbFile(Config.PATH_HR_STORAGE+log_start_time + Config.fileNameEnd , log_start_time);
                break;
            case R.id.bt_clear_db:
                HrInfoDao hrInfoDao = MixApp.getDaoSession(ChartActivity.this).getHrInfoDao();
                QueryBuilder<HrInfo> qd = hrInfoDao.queryBuilder();
                hrInfoDao.deleteInTx(qd.list());
                Logger.e(Logger.DEBUG_TAG,"clear_db,success");
                break;
        }
    }
    public void disconnectAllDevice(){
        if(heartRateService == null){
            return;
        }
        //
        List<BleAdapterBean> beansList = heartRateService.getBeansList();
        if(beansList == null || beansList.size() == 0)
            return;
        for(BleAdapterBean bean : beansList){
            if(!TextUtils.isEmpty(bean.getBleMacName())){
                heartRateService.getmBleManager().disconnect(bean.getBleMacName());
            }
        }

    }


    private void backupDatabase(){
        try {
            String inFileName =  "/data/data/com.dyq.bletest/databases/realm.db";
            File dbFile = new File(inFileName);
            if(!dbFile.exists()){
                Logger.e(Logger.DEBUG_TAG,"FileNotExist!"+inFileName);
                return;
            }
            FileInputStream fis = new FileInputStream(dbFile);
            String outFileName = Config.PATH_HR_STORAGE +"realm.db";
            OutputStream output = new FileOutputStream(outFileName);
            byte[] buffer = new byte[1024];
            int length;
            while((length = fis.read(buffer))>0){
                output.write(buffer,0,length);
            }
            output.flush();
            output.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 把数据库文件写成
     */
    private void writeSDcardFromDbFile(final String filePath, final String startTime){
        hrInfoOperatorHelper.getInstance().asnycGetAllLogRecord(ChartActivity.this, startTime, new AsyncOperationListener() {
            @Override
            public void onAsyncOperationCompleted(AsyncOperation operation) {
                if(operation.getResult() == null){
                    Logger.e(Logger.DEBUG_TAG,"operation.getResult() == null");
                    return;
                }
                List<HrInfo> ll = (List<HrInfo>) operation.getResult();
                if(ll==null || ll.size() == 0){
                    Logger.e(Logger.DEBUG_TAG,"ll.size() ==0");
                    return;
                }
                String totalContent = "";
                String itemContent;
                List<String> totalDeviceNameList = new ArrayList<>();
                for(int i =0;i<ll.size();i++){
                    HrInfo info = ll.get(i);
                    itemContent = "";
                    itemContent += (info.getTime()+",");
                    itemContent += (info.getMac_address()+",");
                    itemContent += (info.getMatter()+",");
                    itemContent += (info.getValue());
                    boolean sameDeviceExist = false;
                    for(String b :totalDeviceNameList){
                        if(b.equals(info.getMac_address())){
                            sameDeviceExist = true;
                            break;
                        }
                    }
                    if(!sameDeviceExist){
                        totalDeviceNameList.add(info.getMac_address());
                    }
                    totalContent += (itemContent + "\r\n");
                }
                String header = "";
                header+=("开始,"+Config.RECORD_START_IDENTIFY+",");
                for(String itemDevice:totalDeviceNameList){
                    header+=(itemDevice+",");
                }
                header+="\r\n";
                FileUtils.writeFile(filePath,header+totalContent);
            }
        });
    }


    /**
     * 现实设备搜索框
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
                    heartRateService.initDeviceColorIdentify();
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
            for(int i =0;i<ll.size();i++) {
                BleAdapterBean adapterBean = ll.get(i);
                hr_compare_chart.addValue(adapterBean.getBleMacName(), new ChartBean(adapterBean.getBleBleHrValue(), heartRateService.getRefreshTime()), true);
            }
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
