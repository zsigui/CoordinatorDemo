package com.luna.powersaver.gp.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by zsigui on 17-1-10.
 */

public class SPUtil {
    public static String getString(Context context, String file, String key, String defValue) {
        if (context == null) {
            return defValue;
        }
        SharedPreferences sp = context.getSharedPreferences(file, Context.MODE_PRIVATE);
        return sp.getString(key, defValue);
    }

    public static int getInt(Context context, String file, String key, int defValue) {
        if (context == null) {
            return defValue;
        }
        SharedPreferences sp = context.getSharedPreferences(file, Context.MODE_PRIVATE);
        return sp.getInt(key, defValue);
    }

    public static long getLong(Context context, String file, String key, long defValue) {
        if (context == null) {
            return defValue;
        }
        SharedPreferences sp = context.getSharedPreferences(file, Context.MODE_PRIVATE);
        return sp.getLong(key, defValue);
    }

    public static boolean getBoolean(Context context, String file, String key, boolean defValue) {
        if (context == null) {
            return defValue;
        }
        SharedPreferences sp = context.getSharedPreferences(file, Context.MODE_PRIVATE);
        return sp.getBoolean(key, defValue);
    }

    public static void putBoolean(Context context, String file, String key, boolean value) {
        try {
            if (context == null) {
                return;
            }
            getEditableSharedPreferences(context, file).putBoolean(key, value).commit();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void putLong(Context context, String file, String key, long value) {
        try {
            if (context == null) {
                return;
            }
            getEditableSharedPreferences(context, file).putLong(key, value).commit();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void putInt(Context context, String file, String key, int value) {
        try {
            if (context == null) {
                return;
            }
            getEditableSharedPreferences(context, file).putInt(key, value).commit();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void putString(Context context, String file, String key, String value) {
        try {
            if (context == null) {
                return;
            }
            getEditableSharedPreferences(context, file).putString(key, value).commit();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static SharedPreferences.Editor getEditableSharedPreferences(Context context, String file) {
        return getSharedPreferences(context, file).edit();
    }

    public static SharedPreferences getSharedPreferences(Context context, String file) {
        return context.getSharedPreferences(file, Context.MODE_PRIVATE);
    }
}