package com.luna.powersaver.gp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.luna.powersaver.gp.manager.BatteryTimeManager;

/**
 * Created by zsigui on 17-1-9.
 */

public class GuardService extends Service {

    public static boolean sIsRunningThisService = false;


    @Override
    public void onCreate() {
        super.onCreate();
        StaticConst.sContext = getApplicationContext();
        Log.d("ps-test", "onBind:GuardService is onCreate!");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("ps-test", "onBind:GuardService is Running!");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIsRunningThisService = true;
        sendWakeUpClock();
        Log.d("ps-test", "onStartCommand:GuardService is Running!");
        if (BatteryTimeManager.get().isCharging()) {
            ViewManager.get().showGuardForce(this, false);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        Log.w("test", "testAliveAndCreateIfNot: isRun = " + sIsRunningThisService);
        if (!sIsRunningThisService) {
            Intent intent = new Intent(context, GuardService.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(intent);
        }
    }
}