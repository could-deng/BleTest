package com.dyq.bletest.bean;

import java.util.List;

/**
 * Created by yuanqiang on 2017/6/16.
 */

public class HrChartBean {
    private String macAddress;
    private List<ChartBean> beanlist;

    public HrChartBean() {
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public List<ChartBean> getBeanlist() {
        return beanlist;
    }

    public void setBeanlist(List<ChartBean> beanlist) {
        this.beanlist = beanlist;
    }

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

}
