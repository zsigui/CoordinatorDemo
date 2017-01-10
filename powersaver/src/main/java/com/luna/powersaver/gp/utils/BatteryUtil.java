package com.luna.powersaver.gp.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Created by zsigui on 17-1-9.
 */

public class BatteryUtil {

    public static final int STATE_QUICK = 0;
    public static final int STATE_CONTINUOUS = 1;
    public static final int STATE_TRICKLE = 2;

    private static int sChargeOneInMillis = 0;
    private static int sLastPercent = 0;
    private static long sLastTimeInMillis = 0;
    private static final String SP_FILE = "powersaver_sp";
    private static final String SP_CHARGE_KEY = "powersaver_key_charge_one";

    public static Intent getBatteryIntent(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        return context.registerReceiver(null, filter);
    }

    public static boolean isCharging(Context context) {
        Intent batteryStatus = getBatteryIntent(context);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    /**
     * 计算剩余充电时间
     */
    public static long calculateRemainTime(Context context) {
        if (sChargeOneInMillis == 0) {
            sChargeOneInMillis = SPUtil.getInt(context, SP_FILE, SP_CHARGE_KEY, -1);
        }
        int percent = getPercent(context);
        Log.d("test", "calculateRemainTime: sLastPercent = " + sLastPercent + ", percent = " + percent
         + ", sChargeOneInMillis = " + sChargeOneInMillis + ", sLastTime = " + sLastTimeInMillis);
        if (sLastPercent == 0) {
            sLastPercent = percent;
        } else if (percent > sLastPercent && sLastTimeInMillis != 0) {
            int diffPercent = percent - sLastPercent;
            long diffTimeInMillis = System.currentTimeMillis() - sLastTimeInMillis;
            int charge = (int) (diffTimeInMillis / diffPercent);
            if (charge > 0 && charge != sChargeOneInMillis) {
                sChargeOneInMillis = charge;
                SPUtil.putInt(context, SP_FILE, SP_CHARGE_KEY, charge);
            }
            sLastPercent = percent;
        }

        sLastTimeInMillis = System.currentTimeMillis();

        if (sChargeOneInMillis > 0) {
            return sChargeOneInMillis * (100 - percent);
        }
        return -1;
    }

    /**
     * 清除设置防止影响下次获取剩余充电时间的判断
     */
    public static void clearCalculateSetting() {
        sChargeOneInMillis = sLastPercent = 0;
        sLastTimeInMillis = 0;
    }

    public static int getPercent(Context context) {
        Intent batteryStatus = getBatteryIntent(context);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        // 保证不溢出以致于超过100%
        level = (level > scale ? scale : level);
        return (int) Math.ceil(level * 100 / scale);
    }

    public static int getChargeState(Context context) {
        int percent = getPercent(context);
        if (percent < 10 || percent > 95) {
            return STATE_TRICKLE;
        } else if (percent > 80) {
            return STATE_CONTINUOUS;
        } else {
            return STATE_QUICK;
        }
    }
}
