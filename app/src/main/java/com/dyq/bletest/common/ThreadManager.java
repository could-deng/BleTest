package com.dyq.bletest.common;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Created by dengyuanqiang on 2017/6/20.
 */

public class ThreadManager {
    /**
     * UI线程 handler
     **/
    private static Handler mUiHandler;

    /**
     *副线程handler
     **/
    private static Handler SUB_THREAD1_HANDLER;

    /**
     * 副线程1
     */
    private static HandlerThread SUB_THREAD1;
    /**
     * 锁
     **/
    private static final Object mMainHandlerLock = new Object();

    /**
     * 取得UI线程Handler
     *
     * @return
     */
    public static Handler getMainHandler() {
        if (mUiHandler == null) {
            synchronized (mMainHandlerLock) {
//                if (mUiHandler == null) {
                mUiHandler = new Handler(Looper.getMainLooper());
//                }
            }
        }
        return mUiHandler;
    }

    /**
     * 获得副线程1的Handler.<br>
     * 副线程可以执行比较快但不能在ui线程执行的操作.<br>
     * 此线程禁止进行网络操作.如果需要进行网络操作.
     * 请使用NETWORK_EXECUTOR</b>
     *
     * @return handler
     */
    public static Handler getSubThread1Handler() {
        if (SUB_THREAD1_HANDLER == null) {
            synchronized (ThreadManager.class) {
                SUB_THREAD1 = new HandlerThread("SUB1");
                SUB_THREAD1.setPriority(Thread.MIN_PRIORITY);//降低线程优先级
                SUB_THREAD1.start();
                SUB_THREAD1_HANDLER = new Handler(SUB_THREAD1.getLooper());
            }
        }
        return SUB_THREAD1_HANDLER;
    }
}
