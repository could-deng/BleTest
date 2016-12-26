package com.dyq.bletest;

/**
 * Created by yuanqiang on 2016/12/5.
 */
public class Config {
    public static final boolean DEBUG = Boolean.parseBoolean("true");

    /** 用户信息模块SharedPreferences文件名 */
    public static final String PREFS_USER = "prefs_user";


    public static final String BLEFILTERNAME = "BLEFILTERNAME";
    /** 用于BLE设备搜索的名称过滤   */
    public static final String BleName = "H10";



    public static final String BlueToothName = "BLUETOOTHNAME";//蓝牙名称
    public static final String BlueToothDefaultName = "LAVA H10";//默认蓝牙名称

    public static final String BleSignalMin = "BLESIGNALMINIUM";//ble信号强度最小值
    public static final int DefaultMinBleSignal = -200;//默认值
    public static final String BleHRMIN = "BLEHRMIN";//BLE心率最小值
    public static final int DEFAULTHRMIN = 0;
    public static final String BleHRMAX = "BLEHRMAX";//BLE心率最大值
    public static final int DEFAULTHRMAX = 200;
    public static final String BLELIGHTINTENSITYMIN = "BLELIGHTINTENSITY";//光强度最小值
    public static final int DEFAULTLIGHTINTENSITYMIN = 0;
    public static final String BleNotAutoConnect = "BLEAUTOCONNECT";//是否自动连接
    public static final boolean DefaultIfNotAutoConnect = false;


    public static int DeviceConnectAndMate = 0;//设备连接并匹配条件
    public static int DeviceSearchedWithoutConnected = 1;//设备搜索到未连接
    public static int DeviceConnectNotMate = 2;//设备连接但未匹配条件

    public static int DeviceDisconnectedBuffer = 3;//点击设备断开，还未执行onDeviceDisconnected（）




    public static int DeviceConnectNumMax = 6;//BLE设备连接数上限
}
