package com.luna.powersaver.gp.utils;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by zsigui on 17-1-23.
 */

public class SystemInfoUtil {


    private static String mManufacturer;
    private static String mImei;

    public static String getManufacturerInfo() {
        try {
            if (TextUtils.isEmpty(mManufacturer)) {
                Field f = Build.class.getField("MANUFACTURER");
                if (f != null) {
                    mManufacturer = f.get(Build.class).toString().trim();
                }
            }
        } catch (Throwable e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        if (TextUtils.isEmpty(mManufacturer)) {
            return Build.BRAND;
        }
        return mManufacturer;
    }

    /**
     * 获取imei地址 2012-11-15
     * 新增放射获取imei的方法
     *
     * @return
     */
    public static String getImei(Context context) {
        try {
            if (TextUtils.isEmpty(mImei)) {
                mImei = getImeiFromSystemApi(context);
            }
            if (TextUtils.isEmpty(mImei)) {
                mImei = getImeiByReflect(context);
            }
        } catch (Throwable t) {
            if (AppDebugLog.IS_DEBUG) {
                t.printStackTrace();
            }
        }
        return "";
    }

    /**
     * 调用系统接口获取imei
     *
     * @param context
     * @return
     */
    private static String getImeiFromSystemApi(Context context) {
        String imei = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                imei = telephonyManager.getDeviceId();
                if (!TextUtils.isEmpty(imei)) {
                    imei = imei.trim();
                    if (imei.contains(" ")) {
                        imei = imei.replace(" ", "");
                    }
                    if (imei.contains("-")) {
                        imei = imei.replace("-", "");
                    }
                    if (imei.contains("\n")) {
                        imei = imei.replace("\n", "");
                    }
                    String meidStr = "MEID:";
                    int stratIndex = imei.indexOf(meidStr);
                    if (stratIndex > -1) {
                        imei = imei.substring(stratIndex + meidStr.length());
                    }
                    imei = imei.trim().toLowerCase();
                    if (imei.length() < 10) {
                        imei = null;
                    }
                }
            }
        } catch (Throwable e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return imei;
    }

    /**
     * 反射调用获取imei
     *
     * @param context
     * @return 真实的imei或者是""
     */
    public static String getImeiByReflect(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getApplicationContext().getSystemService(Context
                    .TELEPHONY_SERVICE);
            // 这里用21标识anroid5.0，因为低版本sdk打包时时没有LOLLIPOP的
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= 21) {
                Method simMethod = TelephonyManager.class.getDeclaredMethod("getDefaultSim");
                Object sim = simMethod.invoke(tm);
                Method method = TelephonyManager.class.getDeclaredMethod("getDeviceId", int.class);
                return method.invoke(tm, sim).toString();
            } else {
                Class<?> clazz = Class.forName("com.android.internal.telephony.IPhoneSubInfo");
                Method subInfoMethod = TelephonyManager.class.getDeclaredMethod("getSubscriberInfo");
                subInfoMethod.setAccessible(true);
                Object subInfo = subInfoMethod.invoke(tm);
                Method method = clazz.getDeclaredMethod("getDeviceId");
                return method.invoke(subInfo).toString();
            }
        } catch (Exception e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return "";
    }
}
