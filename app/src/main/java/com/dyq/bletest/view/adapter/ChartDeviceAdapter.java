package com.dyq.bletest.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.dyq.bletest.R;
import com.dyq.bletest.bean.BleAdapterBean;
import java.util.List;

/**
 * Created by yuanqiang on 2017/6/16.
 */

public class ChartDeviceAdapter extends BaseAdapter{
    LayoutInflater mInflater;
    volatile List<BleAdapterBean> beanList;
    Context context;

    public ChartDeviceAdapter(Context mContext) {
        context = mContext;
        mInflater = LayoutInflater.from(mContext);
        beanList = null;
    }

    public void  setBeanList(List<BleAdapterBean> beanList){
        if((this.beanList!=null) &&(beanList!=this.beanList)){
            this.beanList.clear();
        }
        this.beanList = beanList;
        this.notifyDataSetChanged();
    }

    public List<BleAdapterBean> getBeanList(){
        return beanList;
    }


    @Override
    public int getCount() {
        if ((getBeanList() == null) || (getBeanList().size() == 0)) {
            return 0;
        } else {
            return beanList.size();
        }
    }

    @Override
    public Object getItem(int i) {
        return beanList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        if(getBeanList() == null || getBeanList().size() == 0)return null;
        ChartDeviceAdapter.DeviceViewHolder holder;
        if(view == null) {
            view = mInflater.inflate(R.layout.layout_chart_device_item,null);
            holder = new DeviceViewHolder();
            holder.view_device_color = view.findViewById(R.id.view_device_color);
            holder.tv_device_name = (TextView) view.findViewById(R.id.tv_device_name);
            holder.tv_device_value = (TextView) view.findViewById(R.id.tv_device_value);
            view.setTag(holder);
        }
        holder = (DeviceViewHolder) view.getTag();
        if(getBeanList().size() <= i){
            return view;
        }
        BleAdapterBean bean = getBeanList().get(i);
        holder.tv_device_name.setText(String.valueOf(bean.getBleDeviceName()));
        holder.tv_device_value.setText(String.valueOf(bean.getBleBleHrValue()));
        return view;
    }

    private class DeviceViewHolder {
        public View view_device_color;
        public TextView tv_device_name;
        public TextView tv_device_value;
    }
}
