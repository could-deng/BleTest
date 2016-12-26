package com.dyq.bletest.bean;

import android.bluetooth.BluetoothDevice;

import com.dyq.bletest.Config;

/**
 * Created by yuanqiang on 2016/12/6.
 */
public class BleAdapterBean {
    private int bleSignal;
    private String bleDeviceName;
    private String bleMacName;
    private int bleShineMode;
    private int bleBleHrValue;
    private int bleLightIntensity;
    private BluetoothDevice device;

    private int flag;//0:连接（默认），1:未连接，2:未满足

    public BleAdapterBean() {

    }

//    public BleAdapterBean(BluetoothDevice device,int ShineMode,int signalValue) {
//        this.device = device;
//        if(device!=null) {
//            this.bleDeviceName = device.getName();
//            this.bleMacName = device.getAddress();
//            this.bleSignal = signalValue;
//            this.bleLightIntensity = 0;
//            this.bleShineMode = ShineMode;
//            this.bleBleHrValue = 0;
//            this.flag = Config.DeviceConnectNotMate;
//        }
//    }


    public BleAdapterBean(BluetoothDevice device,int ShineMode,int signalValue,int flag) {
        this.device = device;
        if(device!=null) {
            this.bleDeviceName = device.getName();
            this.bleMacName = device.getAddress();
            this.bleSignal = signalValue;
            this.bleLightIntensity = 0;
            this.bleShineMode = ShineMode;
            this.bleBleHrValue = 0;
            this.flag = flag;
        }
    }

    public int getBleSignal() {
        return bleSignal;
    }

    public void setBleSignal(int bleSignal) {
        this.bleSignal = bleSignal;
    }

    public String getBleDeviceName() {
        return bleDeviceName;
    }

    public String getBleMacName() {
        return bleMacName;
    }


    public void setBleShineMode(int bleShineMode) {
        this.bleShineMode = bleShineMode;
    }

    public int getBleShineMode() {
        return bleShineMode;
    }

    public int getBleBleHrValue() {
        return bleBleHrValue;
    }

    public void setBleBleHrValue(int bleBleHrValue) {
        this.bleBleHrValue = bleBleHrValue;
    }

    public int getBleLightIntensity() {
        return bleLightIntensity;
    }

    public void setBleLightIntensity(int bleLightIntensity) {
        this.bleLightIntensity = bleLightIntensity;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}
