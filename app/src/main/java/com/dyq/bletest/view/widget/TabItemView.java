package com.dyq.bletest.view.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.dyq.bletest.R;


/**
 * ViewPagerIndicator 上显示是否有新信息
 */
public class TabItemView extends FrameLayout {
    RadioButton tabTitle; //ViewPagerIndicator的title 显示
    ImageView messageView;//显示小圆点的提示信息
    TextView badgeView;//可以显示带数字的提示信息
    onCheckedListener mCheckedListener; //选中监听 用于切换页面

    public TabItemView(Context context) {
        super(context);
        initView(context);
    }

    public TabItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //1,获取控件
        initView(context);
    }

    public void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_item_view, this);
        badgeView = (TextView) view.findViewById(R.id.tv_badge_view);
        messageView = (ImageView) view.findViewById(R.id.iv_message_view);
        tabTitle = (RadioButton) view.findViewById(R.id.rb_tabTitle);
    }


    public void setColor(int[] color, float textSize) {
        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_checked}, // unchecked
                new int[]{android.R.attr.state_checked}  // checked
        };

        ColorStateList radioButtonTextColor = new ColorStateList(states, color);
        tabTitle.setTextColor(radioButtonTextColor);
        tabTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

        tabTitle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) return;
                mCheckedListener.onChecked(true);
            }
        });
    }

    public void setTitle(CharSequence text) {
        tabTitle.setText(text);
    }


    /**
     * 显示消息
     *
     * @param showBadge true 显示 false 不显示
     */
    public void showBadge(boolean showBadge) {
        if (showBadge) {
            messageView.setVisibility(View.VISIBLE);
        } else {
            messageView.setVisibility(View.GONE);
        }
        badgeView.setVisibility(View.GONE);
    }

    /**
     * 显示消息数目
     *
     * @param count 消息数目
     */
    public void showBadgeCount(int count) {
        messageView.setVisibility(View.GONE);
        badgeView.setText(count);
    }

    public interface onCheckedListener {
        void onChecked(boolean isChecked);
    }

    public void setOnCheckedListener(onCheckedListener listener) {
        this.mCheckedListener = listener;
    }

    public boolean isChecked() {
        return tabTitle.isChecked();
    }

    public void setChecked(boolean checked) {
        if (tabTitle.isChecked() != checked) {
            tabTitle.setChecked(checked);
        }
    }
} 