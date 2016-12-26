package com.dyq.bletest.common;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SharedPreferences帮助类
 */
public class PrefsHelper {

    private static SharedPreferences sharedPreferences;
    private static PrefsHelper prefsInstance;

    private static final String LENGTH = "_length";

    private static final String DEFAULT_STRING_VALUE = "";
    private static final int DEFAULT_INT_VALUE = -1;
    private static final double DEFAULT_DOUBLE_VALUE = -1d;
    private static final float DEFAULT_FLOAT_VALUE = -1f;
    private static final long DEFAULT_LONG_VALUE = -1L;
    private static final boolean DEFAULT_BOOLEAN_VALUE = false;

    private PrefsHelper(@NonNull Context context, String preferencesName){
            sharedPreferences = context.getApplicationContext().getSharedPreferences(
                    preferencesName,
                    Context.MODE_PRIVATE
            );
    }

    /**
     * 根据context和SharedPreferences文件名返回SharedPreferences实例
     * @param context 上下文
     * @param preferencesName SharedPreferences文件名
     *
     * @return SharedPreferences实例
     */
    public static PrefsHelper with(@NonNull Context context,String preferencesName) {
        prefsInstance = new PrefsHelper(context,preferencesName);
        return prefsInstance;
    }

    //region ======================================== String related methods ========================================

    /**
     * @param what
     * @return Returns the stored value of 'what'
     */
    public String read(String what) {
        return sharedPreferences.getString(what, DEFAULT_STRING_VALUE);
    }

    /**
     * @param what
     * @param defaultString
     * @return Returns the stored value of 'what'
     */
    public String read(String what, String defaultString) {
        return sharedPreferences.getString(what, defaultString);
    }

    /**
     * @param where
     * @param what
     */
    public void write(String where, String what) {
        sharedPreferences.edit().putString(where, what).apply();
    }

    //endregion ======================================== String related methods ========================================

    //region ======================================== int related methods ========================================

    /**
     * @param what
     * @return Returns the stored value of 'what'
     */
    public int readInt(String what) {
        return sharedPreferences.getInt(what, DEFAULT_INT_VALUE);
    }

    /**
     * @param what
     * @param defaultInt
     * @return Returns the stored value of 'what'
     */
    public int readInt(String what, int defaultInt) {
        return sharedPreferences.getInt(what, defaultInt);
    }

    /**
     * @param where
     * @param what
     */
    public void writeInt(String where, int what) {
        sharedPreferences.edit().putInt(where, what).apply();
    }

    //endregion ======================================== int related methods ========================================

    //region ======================================== double related methods ========================================

    /**
     * @param what
     * @return Returns the stored value of 'what'
     */
    public double readDouble(String what) {
        if (!contains(what))
            return DEFAULT_DOUBLE_VALUE;
        return Double.longBitsToDouble(readLong(what));
    }

    /**
     * @param what
     * @param defaultDouble
     * @return Returns the stored value of 'what'
     */
    public double readDouble(String what, double defaultDouble) {
        if (!contains(what))
            return defaultDouble;
        return Double.longBitsToDouble(readLong(what));
    }

    /**
     * @param where
     * @param what
     */
    public void writeDouble(String where, double what) {
        writeLong(where, Double.doubleToRawLongBits(what));
    }

    //endregion ======================================== double related methods ========================================

    //region ======================================== float related methods ========================================
    /**
     * @param what
     * @return Returns the stored value of 'what'
     */
    public float readFloat(String what) {
        return sharedPreferences.getFloat(what, DEFAULT_FLOAT_VALUE);
    }

    /**
     * @param what
     * @param defaultFloat
     * @return Returns the stored value of 'what'
     */
    public float readFloat(String what, float defaultFloat) {
        return sharedPreferences.getFloat(what, defaultFloat);
    }

    /**
     * @param where
     * @param what
     */
    public void writeFloat(String where, float what) {
        sharedPreferences.edit().putFloat(where, what).apply();
    }
    //endregion ======================================== float related methods ========================================

    //region ======================================== long related methods ========================================
    /**
     * @param what
     * @return Returns the stored value of 'what'
     */
    public long readLong(String what) {
        return sharedPreferences.getLong(what, DEFAULT_LONG_VALUE);
    }

    /**
     * @param what
     * @param defaultLong
     * @return Returns the stored value of 'what'
     */
    public long readLong(String what, long defaultLong) {
        return sharedPreferences.getLong(what, defaultLong);
    }

    /**
     * @param where
     * @param what
     */
    public void writeLong(String where, long what) {
        sharedPreferences.edit().putLong(where, what).apply();
    }
    //endregion ======================================== long related methods ========================================

    //region ======================================== boolean related methods ========================================
    /**
     * @param what
     * @return Returns the stored value of 'what'
     */
    public boolean readBoolean(String what) {
        return sharedPreferences.getBoolean(what, DEFAULT_BOOLEAN_VALUE);
    }

    /**
     * @param what
     * @param defaultBoolean
     * @return Returns the stored value of 'what'
     */
    public boolean readBoolean(String what, boolean defaultBoolean) {
        return sharedPreferences.getBoolean(what, defaultBoolean);
    }

    /**
     * @param where
     * @param what
     */
    public void writeBoolean(String where, boolean what) {
        sharedPreferences.edit().putBoolean(where, what).apply();
    }
    //endregion ======================================== boolean related methods ========================================

    //region ======================================== String set related methods ========================================
    /**
     * @param key
     * @param value
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void putStringSet(final String key, final Set<String> value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            sharedPreferences.edit().putStringSet(key, value).apply();
        } else {
            // Workaround for pre-HC's lack of StringSets
            putOrderedStringSet(key, value);
        }
    }

    /**
     * @param key
     * @param value
     */
    public void putOrderedStringSet(String key, Set<String> value) {
        int stringSetLength = 0;
        if (sharedPreferences.contains(key + LENGTH)) {
            // First read what the value was
            stringSetLength = readInt(key + LENGTH);
        }
        writeInt(key + LENGTH, value.size());
        int i = 0;
        for (String aValue : value) {
            write(key + "[" + i + "]", aValue);
            i++;
        }
        for (; i < stringSetLength; i++) {
            // Remove any remaining values
            remove(key + "[" + i + "]");
        }
    }

    /**
     * @param key
     * @param defValue
     * @return Returns the String Set with HoneyComb compatibility
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public Set<String> getStringSet(final String key, final Set<String> defValue) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return sharedPreferences.getStringSet(key, defValue);
        } else {
            // Workaround for pre-HC's missing getStringSet
            return getOrderedStringSet(key, defValue);
        }
    }

    /**
     * @param key
     * @param defValue
     * @return Returns the ordered String Set
     */
    public Set<String> getOrderedStringSet(String key, final Set<String> defValue) {
        if (contains(key + LENGTH)) {
            LinkedHashSet<String> set = new LinkedHashSet<>();
            int stringSetLength = readInt(key + LENGTH);
            if (stringSetLength >= 0) {
                for (int i = 0; i < stringSetLength; i++) {
                    set.add(read(key + "[" + i + "]"));
                }
            }
            return set;
        }
        return defValue;
    }

    //endregion ======================================== String set related methods ========================================

    /**
     * @param key
     */
    public void remove(final String key) {
        if (contains(key + LENGTH)) {
            // Workaround for pre-HC's lack of StringSets
            int stringSetLength = readInt(key + LENGTH);
            if (stringSetLength >= 0) {
                sharedPreferences.edit().remove(key + LENGTH).apply();
                for (int i = 0; i < stringSetLength; i++) {
                    sharedPreferences.edit().remove(key + "[" + i + "]").apply();
                }
            }
        }
        sharedPreferences.edit().remove(key).apply();
    }

    /**
     * 查询SharedPreferences中是否包含指定的键
     * @param key 要查询的键
     *
     * @return true:包含,false:不包含
     */
    public boolean contains(final String key) {
        return sharedPreferences.contains(key);
    }

    /**
     * 清除所有保存的值
     */
    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
