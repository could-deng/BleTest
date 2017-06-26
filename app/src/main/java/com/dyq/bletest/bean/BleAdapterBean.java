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

    private int pushBtnType;//按键消息，-1为无，0为侧面心率按键，1为F按键
    private int msgType;//按键类型，-1为无，0位单击
    private int lightShowTime;//颜色显示时间

    private boolean hrBtnWork;//
    private boolean fBtnWork;

    private int colorIdentify;//chartActivity中adapter中颜色标示

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
            pushBtnType = -1;
            msgType = -1;
            lightShowTime = 0;
            hrBtnWork = false;
            fBtnWork = false;
            colorIdentify = -1;
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

    public int getPushBtnType() {
        return pushBtnType;
    }

    public void setPushBtnType(int pushBtnType) {
        this.pushBtnType = pushBtnType;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public int getLightShowTime() {
        return lightShowTime;
    }

    public void setLightShowTime(int lightShowTime) {
        this.lightShowTime = lightShowTime;
    }


    public boolean isHrBtnWork() {
        return hrBtnWork;
    }

    public void setHrBtnWork(boolean hrBtnWork) {
        this.hrBtnWork = hrBtnWork;
    }

    public boolean isfBtnWork() {
        return fBtnWork;
    }

    public void setfBtnWork(boolean fBtnWork) {
        this.fBtnWork = fBtnWork;
    }

    public int getColorIdentify() {
        return colorIdentify;
    }

    public void setColorIdentify(int colorIdentify) {
        this.colorIdentify = colorIdentify;
    }
}
