package com.dyq.bletest.view.widget;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

/**
 * 自定义View的帮助类,提供如尺寸转换等工具方法
 */
public class ViewUtils {

    /**
     * dp转px
     * @param
     * */
    public static float dp2px(Resources resources, float dp) {
        final float scale = resources.getDisplayMetrics().density;
        return  dp * scale + 0.5f;
    }

    public static float sp2px(Resources resources, float sp){
        final float scale = resources.getDisplayMetrics().scaledDensity;
        return sp * scale;
    }

    public static int dp2px(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static float px2sp(Context context, int px) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }

    /**
     * 获取屏幕的宽度
     * @return 屏幕的像素宽度
     * */
    public static int getScreenWidth(Context context){
        if(context == null)
            return 720;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if(dm == null)
            return 720;
        return dm.widthPixels;
    }

    /**
     * 获取屏幕的密度
     * @return 屏幕的像素密度
     * */
    public static float getScreenDensity(Context context){
        if(context == null)
            return 2;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if(dm == null)
            return 720;
        return dm.density;
    }

    /**
     * 获取屏幕的高度
     * @return 屏幕的像素高度
     * */
    public static int getScreenHeight(Context context){
        if(context == null)
            return 1280;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if(dm == null)
            return 1280;
        return dm.heightPixels;
    }

    /**
     * 根据屏幕宽度以及宽高比动态设置控件高度
     *
     * @param view 要动态设置高度的控制
     * @param ratio 宽高比
     * @param defaultHeight 默认高度,单位为像素
     * */
    public static void setViewHeightByRatio(View view,float ratio,int defaultHeight){
        if(view == null)
            return;

        int mScreenWidth = getScreenWidth(view.getContext());

        int height = (int) (mScreenWidth / ratio);
        if(height <= 0){
            height = defaultHeight;
        }

        ViewGroup.LayoutParams para = view.getLayoutParams();
        if(para != null) {
            para.height = height;
            view.setLayoutParams(para);
        }
    }

}
