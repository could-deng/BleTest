package com.dyq.bletest.bean;

/**
 * Created by yuanqiang on 2017/6/19.
 */

public class ChartBean{
    private int hr;
    private long time;

    public ChartBean(int hr, long time) {
        this.hr = hr;
        this.time = time;
    }

    public int getHr() {
        return hr;
    }

    public void setHr(int hr) {
        this.hr = hr;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}