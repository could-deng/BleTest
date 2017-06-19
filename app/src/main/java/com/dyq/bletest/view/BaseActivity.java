package com.dyq.bletest.view;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.dyq.bletest.R;
import com.dyq.bletest.view.widget.AppMsg;

/**
 * Created by yuanqiang on 2016/12/5.
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * AppMsg上一次提示的时间
     */
    private long lastShowTime;
    /**
     * AppMsg上一次提示的消息
     */
    private String lastMsg;

    /**
     * 在窗口顶部弹出信息
     *
     * @param msg   消息
     * @param style AppMsg.STYLE_ALERT   AppMsg.STYLE_CONFIRM   AppMsg.STYLE_INFO三选一
     */
    public void showAppMessage(String msg, AppMsg.Style style) {
        if (TextUtils.isEmpty(msg))
            msg = getResources().getString(R.string.rsp_error_unknown_error);
        showAppMessage(msg, style, Gravity.TOP);
    }

    /**
     * 在窗口顶部弹出信息
     *
     * @param strResId 消息内容字符串资源id
     * @param style    AppMsg.STYLE_ALERT   AppMsg.STYLE_CONFIRM   AppMsg.STYLE_INFO三选一
     */
    protected void showAppMessage(int strResId, AppMsg.Style style) {
        showAppMessage(getResources().getString(strResId), style, Gravity.TOP);
    }

    /**
     * 在窗口顶部弹出信息
     *
     * @param msg     消息
     * @param style   AppMsg.STYLE_ALERT/AppMsg.STYLE_CONFIRM/AppMsg.STYLE_INFO三选一
     * @param gravity 消息显示位置,如Gravity.BOTTOM,默认显示在顶部
     */
    protected void showAppMessage(String msg, AppMsg.Style style, int gravity) {
        if (TextUtils.isEmpty(msg))
            return;
        long now = System.currentTimeMillis();

        if (((now - lastShowTime) < 2000) && msg.equals(lastMsg)) {//防止短时间内同一消息内容提示多次
            lastShowTime = now;
            lastMsg = msg;
            return;
        }
        AppMsg appMsg = AppMsg.makeText(this, msg, style);
        appMsg.setAnimation(R.anim.app_msg_in, R.anim.app_msg_out);
        //(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        //appMsg.setParent(mAltParent);//用于显示在指定父布局内
        appMsg.setLayoutGravity(gravity);
        appMsg.show();
        lastShowTime = now;
        lastMsg = msg;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        switch (item.getItemId()){
            case R.id.ChartActivity:
                intent.setClass(BaseActivity.this,ChartActivity.class);
                startActivity(intent);
                break;
            case R.id.MainActivity:
                intent.setClass(BaseActivity.this,MainActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
