package com.dyq.bletest.model.database;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table "HR_INFO".
 */
public class HrInfo {

    private Long id;
    private String mac_address;
    private String identify_start_time;
    private String time;
    private String matter;
    private String value;

    public HrInfo() {
    }

    public HrInfo(Long id) {
        this.id = id;
    }

    public HrInfo(Long id, String mac_address, String identify_start_time, String time, String matter, String value) {
        this.id = id;
        this.mac_address = mac_address;
        this.identify_start_time = identify_start_time;
        this.time = time;
        this.matter = matter;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 蓝牙设备mac地址
     */
    public String getMac_address() {
        return mac_address;
    }

    /**
     * 蓝牙设备mac地址
     */
    public void setMac_address(String mac_address) {
        this.mac_address = mac_address;
    }

    /**
     * 记录开始时间,记录唯一标示
     */
    public String getIdentify_start_time() {
        return identify_start_time;
    }

    /**
     * 记录开始时间,记录唯一标示
     */
    public void setIdentify_start_time(String identify_start_time) {
        this.identify_start_time = identify_start_time;
    }

    /**
     * 记录下来的时间
     */
    public String getTime() {
        return time;
    }

    /**
     * 记录下来的时间
     */
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * 事件关键字
     */
    public String getMatter() {
        return matter;
    }

    /**
     * 事件关键字
     */
    public void setMatter(String matter) {
        this.matter = matter;
    }

    /**
     * 值
     */
    public String getValue() {
        return value;
    }

    /**
     * 值
     */
    public void setValue(String value) {
        this.value = value;
    }

}
