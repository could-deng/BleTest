package com.dyq.bletest.view.widget;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.text.TextUtilsCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.dyq.bletest.Config;
import com.dyq.bletest.R;
import com.dyq.bletest.common.PrefsHelper;

/**
 * Created by yuanqiang on 2016/12/12.
 */
public class SettingDialog extends DialogFragment {
    EditText et_bluetooth_name ;
    EditText et_ble_signal_value_min;
    EditText et_ble_hr_value_min;
    EditText et_ble_hr_value_max;
    EditText et_ble_light_intensity_min;

    CheckBox rb_auto_connect;
    Button bt_setting_sure;
    Button bt_setting_cancel;

    public interface ConnectFilter{
//        void setConnectFilter(String deviceName,int minRssi);
//        void setPassFilter(int HrMin,int HrMax,int LightIntensityMin);
        void setFreshData(String deviceName,int minRssi,int HrMin,int HrMax,int LightIntensityMin,boolean notAutoConnect,boolean restartService);
    }
    private ConnectFilter filter;

    public void setFilter(ConnectFilter filter) {
        this.filter = filter;
    }

    public static String BLENAME = "BLENAME";
    public static String BLESIGNALMIN = "BLESIGNALMIN";
    public static String HRMIN = "HRMIN";
    public static String HRMAX = "HRMAX";
    public static String LIGHTINTENSITYMIN = "LIGHTINTENSITYMIN";
    public static String NOTAUTOCONNECT ="AUTOCONNECT";

    private String bleName;
    private int BleSignalMin;
    private int HrMin;
    private int HrMax;
    private int LightIntensityMin;
    private boolean autoConnect;

    public static SettingDialog getInstance(String BleName, int BleSignalMin, int HrMin,int HrMax,int LightIntensityMin,boolean notautoConnect){
        //通过Bundle保存数据
        Bundle args = new Bundle();
        args.putString(BLENAME, BleName);
        args.putInt(BLESIGNALMIN, BleSignalMin);
        args.putInt(HRMIN, HrMin);
        args.putInt(HRMAX, HrMax);
        args.putInt(LIGHTINTENSITYMIN, LightIntensityMin);
        args.putBoolean(NOTAUTOCONNECT,notautoConnect);
        SettingDialog fragment = new SettingDialog();
        //将Bundle设置为fragment的参数
        fragment.setArguments(args);
        return fragment;
    }

    public static SettingDialog getInstance(){
        SettingDialog fragment = new SettingDialog();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_hr_setting, container,false);
        et_bluetooth_name = (EditText) v.findViewById(R.id.et_bluetooth_name);
        et_ble_signal_value_min = (EditText) v.findViewById(R.id.et_ble_signal_value_min);
        et_ble_hr_value_min = (EditText) v.findViewById(R.id.et_ble_hr_value_min);
        et_ble_hr_value_max = (EditText) v.findViewById(R.id.et_ble_hr_value_max);
        et_ble_light_intensity_min = (EditText) v.findViewById(R.id.et_ble_light_intensity_min);

        rb_auto_connect = (CheckBox) v.findViewById(R.id.rb_auto_connect);

        bt_setting_sure = (Button) v.findViewById(R.id.bt_setting_sure);
        bt_setting_cancel = (Button) v.findViewById(R.id.bt_setting_cancel);
        bt_setting_sure.setOnClickListener(onClickListener);
        bt_setting_cancel.setOnClickListener(onClickListener);

        bleName = getArguments().getString(BLENAME);
        BleSignalMin = getArguments().getInt(BLESIGNALMIN);
        HrMin = getArguments().getInt(HRMIN);
        HrMax = getArguments().getInt(HRMAX);
        LightIntensityMin =  getArguments().getInt(LIGHTINTENSITYMIN);
        autoConnect = getArguments().getBoolean(NOTAUTOCONNECT, false);


        et_bluetooth_name.setText(bleName);
        et_ble_signal_value_min.setText(String.valueOf(BleSignalMin));
        et_ble_hr_value_min.setText(String.valueOf(HrMin));
        et_ble_hr_value_max.setText(String.valueOf(HrMax));
        et_ble_light_intensity_min.setText(String.valueOf(LightIntensityMin));
        rb_auto_connect.setChecked(!autoConnect);

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        setStyle(STYLE_NO_TITLE, R.style.AppCompatAlertDialogSlide);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.bt_setting_sure:
                    if(filter!=null){
                        //TODO 是否更改了
//                        if(et_bluetooth_name.getText().toString()!="" && et_ble_signal_value_min.getText().toString()!="") {
//                            filter.setConnectFilter(et_bluetooth_name.getText().toString(), Integer.valueOf(et_ble_signal_value_min.getText().toString()));
//                        }else{
//                            Toast.makeText(getActivity(),"输入蓝牙名称或者信号强度有误",Toast.LENGTH_SHORT).toString();
//                        }
//                        if(et_ble_hr_value_min.getText().toString()!="" && et_ble_hr_value_max.getText().toString()!="" && et_ble_light_intensity_min.getText().toString()!=""){
//                            filter.setPassFilter(Integer.valueOf(et_ble_hr_value_min.getText().toString()), Integer.valueOf(et_ble_hr_value_max.getText().toString()), Integer.valueOf(et_ble_light_intensity_min.getText().toString()));
//                        }else{
//                            Toast.makeText(getActivity(),"输入HrMin、HrMax、LightIntensity有误",Toast.LENGTH_SHORT).toString();
//                        }
                        if(!TextUtils.isEmpty(et_bluetooth_name.getText().toString()) && !TextUtils.isEmpty(et_ble_signal_value_min.getText().toString())) {
                            String hr_min = et_ble_hr_value_min.getText().toString();
                            String hr_max = et_ble_hr_value_max.getText().toString();
                            String light_min = et_ble_light_intensity_min.getText().toString();
                            if(!TextUtils.isEmpty(hr_min) && !TextUtils.isEmpty(hr_max) && !TextUtils.isEmpty(light_min)){
                                boolean restartService = true;
                                if(et_bluetooth_name.getText().toString().equals(bleName) && Integer.valueOf(et_ble_signal_value_min.getText().toString()).equals(BleSignalMin)){
                                    restartService = false;
                                }
                                if(!TextUtils.isEmpty(hr_min) && !TextUtils.isEmpty(hr_max) && !TextUtils.isEmpty(light_min)) {
                                    filter.setFreshData(et_bluetooth_name.getText().toString(), Integer.valueOf(et_ble_signal_value_min.getText().toString()),
                                            Integer.valueOf(et_ble_hr_value_min.getText().toString()), Integer.valueOf(et_ble_hr_value_max.getText().toString()),
                                            Integer.valueOf(et_ble_light_intensity_min.getText().toString()), !rb_auto_connect.isChecked(), restartService);
                                }else{
                                    Toast.makeText(getActivity(),"输入HrMin、HrMax、LightIntensity有误",Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                Toast.makeText(getActivity(),"输入HrMin、HrMax、LightIntensity有误",Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Toast.makeText(getActivity(),"输入蓝牙名称或者信号强度有误",Toast.LENGTH_SHORT).show();
                        }

                    }
                    dismiss();
                    break;

                case R.id.bt_setting_cancel:
                    dismiss();
                    break;
            }
        }
    };
}
