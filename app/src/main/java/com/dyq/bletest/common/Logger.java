package com.dyq.bletest.common;


import android.util.Log;

import com.dyq.bletest.Config;

/**
 * 统一Log日志管理,要修改日志等级,运行如下所示指令:
 * <p>
 *
 * <pre>
 *   adb shell setprop log.tag.fitmix &lt;LOGLEVEL>
 *   LOGLEVEL 表示以下几个值之一:
 *     VERBOSE, DEBUG, INFO, WARN or ERROR
 * </pre>
 * <p>
 * 默认日志的等级是DEBUG.
 * <p>
 * 在release版本日志默认是关闭的,如果要开启,更改{@link #ENABLE_LOGS_IN_RELEASE}的值.
 *
 */
public final class Logger {

    /**
     * 日志默认TAG
     */
    public static final String LOG_TAG = "fitmix";

    /**
     * 日志调试TAG
     * */
    public static final String DEBUG_TAG = "TT";

    /**
     * 是否允许在release版本显示日志
     */
    private static final boolean ENABLE_LOGS_IN_RELEASE = false;

    public static boolean canLog(int level) {
        return (ENABLE_LOGS_IN_RELEASE || Config.DEBUG) && Log.isLoggable(LOG_TAG, level);
    }

    public static void d(String tag, String message) {
        if (canLog(Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

    public static void v(String tag, String message) {
        if (canLog(Log.VERBOSE)) {
            Log.v(tag, message);
        }
    }

    public static void i(String tag, String message) {
        if (canLog(Log.INFO)) {
            Log.i(tag, message);
        }
    }

    public static void i(String tag, String message, Throwable thr) {
        if (canLog(Log.INFO)) {
            Log.i(tag, message, thr);
        }
    }

    public static void w(String tag, String message) {
        if (canLog(Log.WARN)) {
            Log.w(tag, message);
        }
    }

    public static void w(String tag, String message, Throwable thr) {
        if (canLog(Log.WARN)) {
            Log.w(tag, message, thr);
        }
    }

    public static void e(String tag, String message) {
        if (canLog(Log.ERROR)) {
            Log.e(tag, message);
        }
    }

    public static void e(String tag, String message, Throwable thr) {
        if (canLog(Log.ERROR)) {
            Log.e(tag, message, thr);
        }
    }
}
