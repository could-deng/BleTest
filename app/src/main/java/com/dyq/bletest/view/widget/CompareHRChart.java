package com.dyq.bletest.view.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import com.dyq.bletest.R;
import com.dyq.bletest.bean.ChartBean;
import com.dyq.bletest.bean.HrChartBean;
import com.dyq.bletest.common.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 心率对比曲线控件
 */

public class CompareHRChart extends View {


    /**
     * 最大心率上限，默认是220-年龄，现在分正常与肥胖
     */
    private int maxHr;
    private int minHr;

    /**
     * 顶部文字区域高度,单位为像素
     */
    private int horizontalPadding = 20;
    private int verticalPadding = 20;

    private List<HrChartBean> mValues;
    private long mStartTime;
    private float xUnit;//X轴方向的每相邻点的间隔
    private float ratio;//每1值心率对应的高度

    private List<HrChartBean> getValueList(){
        if(mValues == null){
            mValues = new ArrayList<>();
        }
        return mValues;
    }

    public void addValueList(List<HrChartBean> list, boolean reSet, boolean ifScreenOff){
        if (mValues == null) {
            mValues = new ArrayList<>();
        }
        if (list == null || list.size() <= 0)
            return;
//        Logger.i(Logger.DEBUG_TAG, "HeartRateLineChart,addValueList(),size=====" + list.size());
//        mValues不应该清空，因为可能断开不划点，再接上心率带，再重新画点
        if (reSet) {
            mValues.clear();
        }
        if(mValues.size()>0) {
            list.addAll(mValues);
        }
        mValues = list;

        if (!ifScreenOff) {//熄屏不重绘
            this.invalidate();
        }

    }

    /**
     * 增加点
     * @param deviceName
     * @param bean
     * @param reDraw
     */
    public void addValue(String deviceName, ChartBean bean, boolean reDraw){
        if (mValues == null) {
            mValues = new ArrayList<>();
        }
        if(mValues.size() == 0) {
            List<ChartBean> beanList = new ArrayList<>();
            beanList.add(bean);
            mValues.add(new HrChartBean(deviceName,beanList));
        }else{
            for(int i =0;i<mValues.size();i++){
                HrChartBean bb = mValues.get(i);
                if(bb.getMacAddress().equals(deviceName)){
                    bb.getBeanlist().add(bean);
                }else{
                    List<ChartBean> beanList = new ArrayList<>();
                    beanList.add(bean);
                    mValues.add(new HrChartBean(deviceName,beanList));
                }
            }
        }
        if(reDraw){
            this.invalidate();
        }
    }


    /**  控件宽度   */
    private int mWidth;
    /** 控件高度    */
    private int mHeight;
    /**画笔    */
    private Paint paint1;
    private Paint paint2;
    private Paint paint3;

    private Paint getPaint1(){
        if(paint1 == null){
            paint1 = new Paint();
            paint1.reset();
            paint1.setAntiAlias(true);
            paint1.setStrokeWidth(2);
            paint1.setStyle(Paint.Style.STROKE);
            paint1.setColor(getResources().getColor(R.color.button1));
        }
        return paint1;
    }
    private Paint getPaint2(){
        if(paint2 == null){
            paint2 = new Paint();
            paint2.reset();
            paint2.setAntiAlias(true);
            paint2.setStrokeWidth(2);
            paint2.setStyle(Paint.Style.STROKE);
            paint2.setColor(getResources().getColor(R.color.button2));
        }
        return paint2;
    }
    private Paint getPaint3(){
        if(paint3 == null){
            paint3 = new Paint();
            paint3.reset();
            paint3.setAntiAlias(true);
            paint3.setStrokeWidth(2);
            paint3.setStyle(Paint.Style.STROKE);
            paint3.setColor(getResources().getColor(R.color.button3));
        }
        return paint3;
    }

    /** 获取画笔
     * @param i
     * @return
     */
    private Paint SpecialListToPaint(int i){
        if(i == 0 ){
            return getPaint1();
        }else if(i == 1){
            return getPaint2();
        }else{
            return getPaint3();
        }
    }

    public CompareHRChart(Context context) {
        super(context);
    }

    public CompareHRChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompareHRChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        mWidth = widthSpecSize;
        mHeight = heightSpecSize;
        setMeasuredDimension(mWidth, mHeight);

        calculateHRInterval(24,70);
        xUnit = (mWidth-horizontalPadding*2)/120f;
        ratio = (mHeight-2*horizontalPadding)*1.0f /(maxHr - minHr);
        Logger.e(Logger.DEBUG_TAG,"xUnit:"+xUnit+",ratio:"+ratio);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawLine(canvas);
    }

    /**
     * 画控件背景
     * @param canvas
     */
    private void drawBackground(Canvas canvas){
        drawLine(canvas,horizontalPadding,mHeight-verticalPadding,mWidth-horizontalPadding,mHeight-verticalPadding,getPaint2());
        drawLine(canvas,horizontalPadding,mHeight-verticalPadding,horizontalPadding,verticalPadding,getPaint2());
    }

    private void init(){
        calculateHRInterval(24,70);
    }

    private void calculateHRInterval(int age,int restHr){
        maxHr = 220-age;
        minHr = restHr - 15;
    }

    /**
     * 画数据点折线
     * @param canvas
     */
    private void drawLine(Canvas canvas){
        if(mValues == null || mValues.size() == 0){
            return;
        }
        if(mStartTime == -1){
            mStartTime = System.currentTimeMillis();
            return;
        }
        for(int k = 0;k<mValues.size();k++) {
            HrChartBean ll = mValues.get(k);
            List<ChartBean> chartBeanList = ll.getBeanlist();
            if(chartBeanList.size() == 0 || chartBeanList.size() == 1){
                continue;
            }
            for (int i = 1; i <= chartBeanList.size(); i++) {
                int j;
                for (j = i - 1; j >= 0; j--) {
                    float startx = (chartBeanList.get(j).getTime() - mStartTime) / 1000f * xUnit;
                    float starty = getYAxisValue(chartBeanList.get(j).getHr(), 2f);
                    float endx = (chartBeanList.get(i).getTime() - mStartTime) / 1000f * xUnit;
                    float endy = getYAxisValue(chartBeanList.get(i).getHr(), 2f);
                    canvas.drawLine((chartBeanList.get(j).getTime() - mStartTime) / 1000f * xUnit, getYAxisValue(chartBeanList.get(j).getHr(), 2f),
                            (chartBeanList.get(i).getTime() - mStartTime) / 1000f * xUnit, getYAxisValue(chartBeanList.get(i).getHr(), 2f), SpecialListToPaint(k));
                }
            }
        }
    }

    /**
     * 从心率值转为Y轴的像素位置
     * @param heartRate
     * @param paintStrokeWidth
     * @return
     */
    private float getYAxisValue(int heartRate,float paintStrokeWidth) {
        if(heartRate<=minHr){
            return mHeight-verticalPadding;
        }
        if(heartRate>=maxHr){
            return verticalPadding;
        }
        return mHeight - verticalPadding - (heartRate - minHr)*ratio - paintStrokeWidth;
    }
    /**
     * 画直线
     * @param canvas 画布
     * @param startX 横向开始坐标
     * @param endX   横向结束坐标
//     * @param color  线条颜色
     */
    private void drawLine(Canvas canvas, int startX, int endX, int startY, int endY, Paint paint) {
        paint.reset();
//        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(startX, endX, startY, endY, paint);
    }
}
