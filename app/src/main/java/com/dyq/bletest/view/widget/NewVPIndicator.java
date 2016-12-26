package com.dyq.bletest.view.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;


import com.dyq.bletest.R;

import java.lang.ref.WeakReference;

import static android.support.v4.view.ViewPager.OnPageChangeListener;


public class NewVPIndicator extends LinearLayout implements OnPageChangeListener {
    private WeakReference<ViewPager> mViewpager;
    private int mIndicatorColor;//指示器颜色
    private float mIndicatorWidth;
    private float mIndicatorHeight;
    private int mTextColor;
    private float mTextSize;
    private int mCurrentPosition = 0;

    private float mTranslationX;
    private int mTabWidth;
    private int mTabCount;
    private String[] mTitles;
    private Paint mPaint;
    public LinearLayout tabGroup;
    private OnTabClickListener mListener;

    public interface OnTabClickListener {
        void onTabClick(int index);
    }

    /**
     * 设置tab点击回调事件
     */
    public void setOnTabClickListener(OnTabClickListener listener) {
        mListener = listener;
    }

    public NewVPIndicator(Context context) {
        super(context);
        if (isInEditMode()) {
            return;
        }
        init(context, null);
    }

    public NewVPIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER);
        if (isInEditMode()) {
            return;
        }
        handleTypedArray(context, attrs);
        mPaint = new Paint();
        mPaint.setColor(mIndicatorColor);
        mPaint.setStrokeWidth(mIndicatorHeight);//9.0f
    }

    private void handleTypedArray(Context context, AttributeSet attrs) {
        TypedArray typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.SimpleViewPagerIndicator);

        mIndicatorColor = typedArray.getColor(R.styleable.SimpleViewPagerIndicator_indicator_color, ContextCompat.getColor(getContext(), R.color.colorAccent));
        mIndicatorWidth = typedArray.getDimension(R.styleable.SimpleViewPagerIndicator_indicator_width, 65);
        mIndicatorWidth = ViewUtils.dp2px(getContext().getResources(), mIndicatorWidth);//dp转px
        mIndicatorHeight = typedArray.getDimension(R.styleable.SimpleViewPagerIndicator_indicator_height, 1);
        mIndicatorHeight = ViewUtils.dp2px(getContext().getResources(), mIndicatorHeight);//dp转px
        mTextColor = typedArray.getColor(R.styleable.SimpleViewPagerIndicator_text_color, ContextCompat.getColor(getContext(), R.color.textColorPrimary));
        mTextSize = typedArray.getDimension(R.styleable.SimpleViewPagerIndicator_text_size, 16);
        typedArray.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mTabCount > 0) {
            mTabWidth = w / mTabCount;
        }
    }

    public void setIndicatorColor(int indicatorColor) {
        this.mIndicatorColor = indicatorColor;
    }

    public void setTitles(String[] Titles) {
        mTitles = Titles;
    }

    /**
     * 显示新消息通知
     *
     * @param index     页面角标
     * @param showBadge 是否显示消息提醒
     */
    public void setMessage(int index, boolean showBadge) {
        ((TabItemView) tabGroup.getChildAt(index)).showBadge(showBadge);
    }

    public void setViewPager(ViewPager viewPager) {
        mViewpager = new WeakReference<>(viewPager);
        if(mViewpager.get() != null) {
            mCurrentPosition = mViewpager.get().getCurrentItem();
            mViewpager.get().addOnPageChangeListener(this);
            onPageSelected(mCurrentPosition);
            mTabCount = mTitles.length;
            if (mTabCount > 0) {
                mTabWidth = getWidth() / mTabCount;
            }
            generateTitleView();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels) {
        scroll(position, positionOffset);
    }

    @Override
    public void onPageSelected(int position) {
        mCurrentPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.save();
        canvas.translate(mTranslationX, getHeight() - 2);
        canvas.drawLine((mTabWidth - mIndicatorWidth) / 2, 0, (mTabWidth + mIndicatorWidth) / 2, 0, mPaint);
        canvas.restore();
    }

    public void scroll(int position, float offset) {
        mTranslationX = getWidth() / mTabCount * (position + offset);
        if (tabGroup != null && position < tabGroup.getChildCount()) {
            if (tabGroup.getChildAt(position) instanceof TabItemView) {
                for (int i = 0; i < tabGroup.getChildCount(); i++) {
                    if (i == position) {//防止一个radioButton 重复选中 会出现混乱
                        final TabItemView tabView = (TabItemView) tabGroup.getChildAt(i);
                        tabView.tabTitle.setOnCheckedChangeListener(null);//移除监听 //除非手动点击的才监听点击事件
                        tabView.setChecked(true);
                        tabView.tabTitle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {//开始监听
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (!isChecked) return;
                                tabView.mCheckedListener.onChecked(true);
                            }
                        });
                    } else {
                        ((TabItemView) tabGroup.getChildAt(i)).setChecked(false);
                    }
                }

            }
        }
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    private void generateTitleView() {
        if (getChildCount() > 0)
            this.removeAllViews();

        //创建TabItemView
        tabGroup = new LinearLayout(getContext());
        tabGroup.setOrientation(RadioGroup.HORIZONTAL);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        tabGroup.setLayoutParams(layoutParams);
        int count = mTitles.length;

        int[] colors = new int[]{
                mTextColor,
                mIndicatorColor
        };
        tabGroup.setWeightSum(count);
        for (int i = 0; i < count; i++) {
            LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
            TabItemView tabItemView = new TabItemView(getContext());
            tabItemView.setTitle(mTitles[i]);
            tabItemView.setColor(colors, mTextSize);
            final int index = i;
            tabItemView.setOnCheckedListener(new TabItemView.onCheckedListener() {
                @Override
                public void onChecked(boolean isChecked) {
                    if (mListener != null) {
                        mListener.onTabClick(index);
                    }
                    if(mViewpager.get() != null) {
                        mViewpager.get().setCurrentItem(index, false);
                    }
                }
            });
            tabGroup.addView(tabItemView, i, lp);
        }
        addView(tabGroup);
    }
}
