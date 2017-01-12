package com.luna.powersaver.gp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import com.luna.powersaver.gp.manager.BatteryTimeManager;
import com.luna.powersaver.gp.manager.SPManager;

/**
 * Created by zsigui on 17-1-12.
 */

public class BatteryChangeReceiver extends BroadcastReceiver {

    public static int sCurrentPercent = -1;
    public static int sCurrentPlug = -1;
    public static boolean sIsQuickCharge;

    public static final String TAG = "ps-test";

    private boolean isFirstQuickCharge = true;
    private boolean isFirstContinuousCharge = true;
    private long lastRecordQuickTime = 0;
    private int lastRecordQuickPercent = 0;
    private long lastRecordContinuousTime = 0;
    private int lastRecordContinuousPercent = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null)
            return;
        GuardService.testAliveAndCreateIfNot(context);

        Log.d(TAG, "PowerReceiver receive! = " + intent.getAction());
        if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            if (BatteryTimeManager.get().isCharging()) {
                // 显示屏幕保护
                ViewManager.get().showNewGuard(context, false);
            }
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {

            // 关闭屏幕保护
            ViewManager.get().hideLastGuard(context);

        } else if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
            // 电量变换中，进行监听判断

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            sCurrentPercent = level * 100 / scale;
            if (sCurrentPercent > 100)
                sCurrentPercent = 100;

            Log.d(TAG, "receiver charge change : " + sCurrentPercent + "%");

            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            SPManager sp = SPManager.get(context);
            int plug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    Log.d(TAG, "start quick charge");
                    if (!sIsQuickCharge) {

                        sIsQuickCharge = true;
                        sp.putRealChargeSpeed(0);

                    }
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    Log.d(TAG, "start full charge");
                    if (sIsQuickCharge) {

                        sIsQuickCharge = false;
                        // 删除冗余并无更多有用的东西
                    }
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    Log.d(TAG, "no charging");
                    if (sIsQuickCharge) {
                        sIsQuickCharge = false;
                    }
                    break;
            }
            long currentTime = System.currentTimeMillis();
            if (sIsQuickCharge) {
                isFirstContinuousCharge = true;
                lastRecordContinuousPercent = 0;
                if (lastRecordQuickPercent == 0) {
                    // 首次计算，记录值
                    lastRecordQuickPercent = sCurrentPercent;
                    lastRecordQuickTime = currentTime;
                } else {
                    int lackPercent = sCurrentPercent - lastRecordQuickPercent;
                    if (lackPercent > 0) {
                        if (isFirstQuickCharge) {
                            isFirstQuickCharge = false;
                        } else {
                            BatteryTimeManager.get().writeBatteryQuickChargeSpeed(
                                    (currentTime - lastRecordQuickTime) / lackPercent,
                                    sCurrentPercent,
                                    plug
                            );
                        }
                        lastRecordQuickTime = currentTime;
                        lastRecordQuickPercent = sCurrentPercent;
                    }
                }
            } else {
                lastRecordQuickPercent = 0;
                isFirstQuickCharge = true;
                if (lastRecordContinuousPercent == 0) {
                    lastRecordContinuousPercent = sCurrentPercent;
                    lastRecordContinuousTime = currentTime;
                } else {
                    int lackPercent = lastRecordQuickPercent - sCurrentPercent;
                    if (lackPercent > 0) {
                        if (isFirstContinuousCharge) {
                            isFirstContinuousCharge = false;
                        } else {
                            BatteryTimeManager.get().writeBatteryFullChargeSpeed(
                                    (currentTime - lastRecordContinuousTime) / lackPercent
                            );
                        }
                        lastRecordContinuousTime = currentTime;
                        lastRecordContinuousPercent = sCurrentPercent;
                    }
                }
            }


        }
    }
}