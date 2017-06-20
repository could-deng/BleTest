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

    public HrChartBean(String macAddress, List<ChartBean> beanlist) {
        this.macAddress = macAddress;
        this.beanlist = beanlist;
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



}
