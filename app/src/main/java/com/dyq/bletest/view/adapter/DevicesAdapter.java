package com.dyq.bletest.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dyq.bletest.Config;
import com.dyq.bletest.R;
import com.dyq.bletest.bean.BleAdapterBean;
import com.dyq.bletest.common.Logger;
import com.dyq.bletest.view.MainActivity;

import java.util.List;

/**
 * Created by yuanqiang on 2016/12/6.
 */
public class DevicesAdapter extends BaseAdapter {
    LayoutInflater mInflater;
    volatile List<BleAdapterBean> beanList;
    Context context;
    public DevicesAdapter(Context mContext) {
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
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if(getBeanList() == null || getBeanList().size() == 0)return null;
        DeviceViewHolder holder;
        if(view == null) {
            view = mInflater.inflate(R.layout.layout_device_item, viewGroup, false);
            holder = new DeviceViewHolder();
            holder.ll_device_item = (LinearLayout) view.findViewById(R.id.ll_device_item);
            holder.tv_ble_signal = (TextView) view.findViewById(R.id.tv_ble_signal);
            holder.tv_ble_hr_value = (TextView) view.findViewById(R.id.tv_ble_hr_value);
            holder.tv_ble_mac_name = (TextView) view.findViewById(R.id.tv_ble_mac_name);
            holder.tv_ble_name = (TextView) view.findViewById(R.id.tv_ble_name);
            holder.tv_ble_photometry = (TextView) view.findViewById(R.id.tv_ble_photometry);
            holder.tv_ble_shine_mode = (TextView) view.findViewById(R.id.tv_ble_shine_mode);
            holder.bt_cut_down = (Button) view.findViewById(R.id.bt_cut_down);
            view.setTag(holder);
        }
        holder = (DeviceViewHolder) view.getTag();
        if(getBeanList().size() <= i){
            return view;
        }
        BleAdapterBean bean = getBeanList().get(i);

        switch (bean.getFlag()) {
            case 0://连接
                holder.ll_device_item.setBackgroundResource(R.color.device_connect);
                break;
            case 1://未连接
                holder.ll_device_item.setBackgroundResource(R.color.device_not_connect);
                break;
            case 2://未满足
                holder.ll_device_item.setBackgroundResource(R.color.device_not_satisfy);
                break;
            case 3:
                holder.ll_device_item.setBackgroundResource(R.color.device_disconnect_buffer);
                break;
        }

        String rssi = bean.getBleSignal() +"dBm";
        holder.tv_ble_signal.setText(rssi);


        String hr = String.format(context.getResources().getString(R.string.h10_hr_avg),bean.getBleBleHrValue());
        holder.tv_ble_hr_value.setText(hr);

        String light_mode = String.format(context.getResources().getString(R.string.h10_light_mode),getLightMode(bean.getBleShineMode()));
        holder.tv_ble_shine_mode.setText(light_mode);

        holder.tv_ble_name.setText(bean.getBleDeviceName());
        holder.tv_ble_mac_name.setText(bean.getBleMacName());

        String photometry = String.format(context.getResources().getString(R.string.h10_hr_light_intensity),bean.getBleLightIntensity());
        holder.tv_ble_photometry.setText(photometry+"%");

        if(bean.getFlag() == Config.DeviceSearchedWithoutConnected){
            holder.bt_cut_down.setVisibility(View.INVISIBLE);
        }else{
            holder.bt_cut_down.setVisibility(View.VISIBLE);
            holder.bt_cut_down.setText("断开");
        }
        if(bean.getFlag() == Config.DeviceDisconnectedBuffer){
            holder.bt_cut_down.setEnabled(false);
            holder.bt_cut_down.setClickable(false);
        }
        holder.bt_cut_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BleAdapterBean bean = getBeanList().get(i);

                if(bean.getFlag() == Config.DeviceConnectAndMate || bean.getFlag() == Config.DeviceConnectNotMate) { //连接状态是已连接状态 ，断开
                    ((MainActivity) context).disconnectDevice(bean.getBleMacName());
                }else {
                    Toast.makeText(context, "条件onClick()不满足，bean.getFlag()：" + bean.getFlag(), Toast.LENGTH_SHORT).show();
                }
//                else if(bean.getFlag() == Config.DeviceSearchedWithoutConnected){ // 连接状态是已加入，但是未连接// 去除
//
//                    ((MainActivity)context).removeDeviceFromList(bean.getBleMacName());
//                }else{
//                    Logger.i(Logger.DEBUG_TAG,"device have not connected");
//                }
            }
        });

        return view;
    }

    private String getLightMode(int mode){
        switch (mode){
            case 1:
                return context.getResources().getString(R.string.light_mode_heart);
            case 2:
                return context.getResources().getString(R.string.light_mode_breath);
            case 5:
                return context.getResources().getString(R.string.light_mode_light_always);
            case -1://mode == -1,代表断开了
                return "- -";
        }
        return "- -";
    }
    private class DeviceViewHolder{
        public LinearLayout ll_device_item;
        public TextView tv_ble_signal;
        public TextView tv_ble_name;
        public TextView tv_ble_mac_name;
        public TextView tv_ble_shine_mode;
        public TextView tv_ble_hr_value;
        public TextView tv_ble_photometry;
        public Button bt_cut_down;
    }
}
