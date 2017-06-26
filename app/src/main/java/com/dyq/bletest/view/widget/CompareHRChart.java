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
     * x轴上的总大小，默认情况下为120秒
     */
    private static final float xSecondSize = 120f;

    /**
     * 顶部文字区域高度,单位为像素
     */
    private static final int horizontalPadding = 20;
    private static final int verticalPadding = 20;

    private List<HrChartBean> mValues;
    private long mStartTime = -1;
    private float xUnit;//X轴方向的每相邻点的间隔
    private float ratio;//每1值心率对应的高度

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
            int existIndex = -1;
            for(int i =0;i<mValues.size();i++){
                HrChartBean bb = mValues.get(i);
                if(bb.getMacAddress().equals(deviceName)){
                    existIndex = i;
                    break;
                }
            }
            if(existIndex != -1){
                HrChartBean bb = mValues.get(existIndex);
                if(bb.getBeanlist().size()>=xSecondSize) {
                    bb.getBeanlist().remove(0);
                }
                bb.getBeanlist().add(bean);
            }else{
                Logger.e(Logger.DEBUG_TAG,deviceName+"新增加");
                List<ChartBean> beanList = new ArrayList<>();
                beanList.add(bean);
                mValues.add(new HrChartBean(deviceName,beanList));
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

    Paint.FontMetricsInt fontMetrics;

    /**画笔    */
    private Paint paintBg;
    private Paint paint1;
    private Paint paint2;
    private Paint paint3;
    private Paint paint4;
    private Paint paint5;

    private Paint getPaintBg(){
        if(paintBg == null){
            paintBg = new Paint();
            paintBg.reset();
            paintBg.setAntiAlias(true);
            paintBg.setStrokeWidth(2);
            paintBg.setStyle(Paint.Style.STROKE);
            paintBg.setColor(getResources().getColor(R.color.chart_bg_line));
        }
        return paintBg;
    }
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
    private Paint getPaint4(){
        if(paint4 == null){
            paint4 = new Paint();
            paint4.reset();
            paint4.setAntiAlias(true);
            paint4.setStrokeWidth(2);
            paint4.setStyle(Paint.Style.STROKE);
            paint4.setColor(getResources().getColor(R.color.button4));
        }
        return paint4;
    }
    private Paint getPaint5(){
        if(paint5 == null){
            paint5 = new Paint();
            paint5.reset();
            paint5.setAntiAlias(true);
            paint5.setStrokeWidth(2);
            paint5.setStyle(Paint.Style.STROKE);
            paint5.setColor(getResources().getColor(R.color.button5));
        }
        return paint5;
    }

    /** 获取画笔
     * @param i
     * @return
     */
    private Paint SpecialListToPaint(int i){
        if(i == 0){
            return getPaint1();
        }else if(i == 1){
            return getPaint2();
        }else if(i == 2){
            return getPaint3();
        }else if(i == 3){
            return getPaint4();
        }else{
            return getPaint5();
        }
    }

    public CompareHRChart(Context context) {super(context,null);}

    public CompareHRChart(Context context, AttributeSet attrs) {
        super(context, attrs,0);
    }

    public CompareHRChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        mWidth = widthSpecSize;
        mHeight = heightSpecSize;
        setMeasuredDimension(mWidth, mHeight);

        calculateHRInterval(250,0);
        xUnit = (mWidth-verticalPadding*2)/xSecondSize;
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
        drawLine(canvas,horizontalPadding,mHeight-verticalPadding,mWidth-horizontalPadding,mHeight-verticalPadding,getPaintBg());
        drawLine(canvas,horizontalPadding,mHeight-verticalPadding,horizontalPadding,verticalPadding,getPaintBg());

        fontMetrics = getPaintBg().getFontMetricsInt();
        float hrTextSize = getPaintBg().measureText(String.valueOf(maxHr));
        canvas.drawText(String.valueOf(maxHr), horizontalPadding - hrTextSize/2,
                (verticalPadding - fontMetrics.bottom - fontMetrics.top)/2f ,getPaintBg());
        float textSize = getPaintBg().measureText(xSecondSize+"秒");
        canvas.drawText(String.valueOf(xSecondSize)+"秒",mWidth-verticalPadding-textSize,
                mHeight - (verticalPadding + fontMetrics.bottom + fontMetrics.top) / 2f,getPaintBg());
    }

    private void calculateHRInterval(int maxHr,int minHr){
        this.maxHr = maxHr;
        this.minHr = minHr;
    }

    /**
     * 画数据点折线
     * @param canvas
     */
    private void drawLine(Canvas canvas){
        if(mValues == null || mValues.size() == 0){
            return;
        }

//        if(mValues.size() == 1){
//            canvas.drawPoint(verticalPadding,getYAxisValue(chartBeanList.get(j).getHr(), 2f));
//        }

        if (mStartTime == -1) {
            mStartTime = mValues.get(0).getBeanlist().get(0).getTime();
            return;
        }

        for(int i =0;i<mValues.size();i++){
            HrChartBean charBean = mValues.get(i);
            if(charBean.getBeanlist().size() >= xSecondSize){
                mStartTime = charBean.getBeanlist().get(0).getTime();//只取一个即可
                break;
            }
        }

        for(int k = 0;k<mValues.size();k++) {
            HrChartBean ll = mValues.get(k);
            List<ChartBean> chartBeanList = ll.getBeanlist();
            if(chartBeanList.size() == 0 || chartBeanList.size() == 1){
                continue;
            }
            for (int i = 1; i < chartBeanList.size(); i++) {
                int j;
                for (j = i - 1; j >= 0; j--) {
                    /** 零的情况也要画图 */
                    float startx = verticalPadding + (chartBeanList.get(j).getTime() - mStartTime) / 1000f * xUnit;
                    float starty = /*horizontalPadding*/ + getYAxisValue(chartBeanList.get(j).getHr(), 2f);
                    float endx = verticalPadding + (chartBeanList.get(i).getTime() - mStartTime) / 1000f * xUnit;
                    float endy = /*horizontalPadding*/ + getYAxisValue(chartBeanList.get(i).getHr(), 2f);
                    canvas.drawLine(startx, starty, endx, endy, SpecialListToPaint(k));
                    break;
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
        return mHeight - verticalPadding - (heartRate - minHr) * ratio - paintStrokeWidth;
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
