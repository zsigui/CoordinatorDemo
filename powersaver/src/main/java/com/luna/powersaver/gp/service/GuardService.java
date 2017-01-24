package com.luna.powersaver.gp.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.manager.ClockManager;
import com.luna.powersaver.gp.receiver.BatteryEventReceiver;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.GuardUtil;

/**
 * Created by zsigui on 17-1-9.
 */

public class GuardService extends Service {

    private static String TAG = GuardService.class.toString();
    public static boolean sIsRunningThisService = false;
    private BatteryEventReceiver mReceiver;
    @Override
    public void onCreate() {
        super.onCreate();
        StaticConst.sContext = getApplicationContext();
        AppDebugLog.d(TAG, "onBind:GuardService is onCreate!");
        mReceiver = new BatteryEventReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
        GuardUtil.closeSystemGuard(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        AppDebugLog.d(TAG, "onBind:GuardService is Running!");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIsRunningThisService = true;
        sendWakeUpClock();
        AppDebugLog.d(TAG, "onStartCommand:GuardService is Running!");
//        if (BatteryTimeManager.get().isCharging()) {
//            PowerSaver.get().showGuardView(this);
//        }
        ClockManager.get().startAlarmImmediately(this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        sIsRunningThisService = false;
        testAliveAndCreateIfNot(this);
    }

    private PendingIntent clockSender;
    public static final String CLOCK_ACTION = "com.luna.powersaver.gp.KEEP_ALIVE";

    private void sendWakeUpClock() {
        if (clockSender == null) {
            Intent intent = new Intent(CLOCK_ACTION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            clockSender = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 2 * 60 * 1000, clockSender);
    }

    private void cancelWakeUpClock() {
        if (clockSender != null) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.cancel(clockSender);
            clockSender = null;
        }
    }

    public static void testAliveAndCreateIfNot(Context context) {
        if (!sIsRunningThisService) {
            Intent intent = new Intent(context, GuardService.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(intent);
        }
    }
}
