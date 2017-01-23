package com.luna.powersaver.gp.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.luna.powersaver.gp.utils.AppDebugLog;

/**
 * 定期闹钟请求网络服务
 * Created by zsigui on 17-1-18.
 */

public class ClockManager {

    public static final String ACTION_CLOCK = "com.luna.powersaver.gp.ACTION_CLOCK";
    public static final String ACTION_OPEN_SPY = "com.luna.powersaver.gp.ACTION_OPEN_RECORD";
    private PendingIntent mSensor;
    // 默认请求网络间隔，单位: minute
    private int mIntervalInMin;
    private long mLastTriggerInMillis = 0;
    private static ClockManager sInstance;

    public static ClockManager get() {
        if (sInstance == null) {
            sInstance = new ClockManager();
        }
        return sInstance;
    }

    /**
     * 初次调用则初始化闹钟，二次调用情况下当间隔变化时重新设置闹钟
     */
    public void startOrResetAlarm(Context context, int interval) {
        if (mSensor == null) {
            startAlarm(context);
        } else if (mLastTriggerInMillis != 0 && interval != 0 && interval != mIntervalInMin) {
            long curTime = System.currentTimeMillis();
            mIntervalInMin = interval;
            cancelAlarm(context);
            initPendingIntent(context);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            long real = curTime - mLastTriggerInMillis;
            real = mIntervalInMin * 60 * 1000 - real;
            AppDebugLog.d(AppDebugLog.TAG_NET, "重设闹钟: real = " + real);
            am.set(AlarmManager.RTC_WAKEUP, curTime + 10 * 1000, mSensor);
            mLastTriggerInMillis = real;
        }
    }

    /**
     * 取消之前闹钟，执行新的闹钟
     */
    public void startAlarm(Context context) {
        AppDebugLog.d(AppDebugLog.TAG_NET, "启动闹钟");
        cancelAlarm(context);
        long curTime = System.currentTimeMillis();
        mIntervalInMin = (mIntervalInMin <= 0 ? 15 : mIntervalInMin);
        initPendingIntent(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC, curTime + mIntervalInMin * 60 * 1000, mSensor);
        mLastTriggerInMillis = curTime;
    }

    private void initPendingIntent(Context context) {
        if (mSensor == null) {
            Intent intent = new Intent();
            intent.setAction(ACTION_CLOCK);
            mSensor = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    public void startAlarmImmediately(Context context) {
        AppDebugLog.d(AppDebugLog.TAG_NET, "启动下一次闹钟");
        cancelAlarm(context);
        long curTime = System.currentTimeMillis();
        initPendingIntent(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, curTime + 1000, mSensor);
        mLastTriggerInMillis = curTime;
    }

    /**
     * 取消已经设置的闹钟
     */
    public void cancelAlarm(Context context) {
        if (mSensor != null) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(mSensor);
            mSensor = null;
        }
    }

    private PendingIntent mOpenSpyIntent;
    private long mLastOpenRecordTime = 0;

    public void startOpenRecordAlarm(Context context) {
        AppDebugLog.d(AppDebugLog.TAG_NET, "开启监听时间闹钟! isNull = " + (mOpenSpyIntent == null));
        if (mOpenSpyIntent != null) {
            return;
        }
        Intent intent = new Intent(ACTION_OPEN_SPY);
        mOpenSpyIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        long curTime = System.currentTimeMillis();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC, curTime + 15 * 1000, mOpenSpyIntent);
        mLastOpenRecordTime = curTime;
        mOpenSpyIntent = null;
    }

    public void stopOpenRecordAlarm(Context context) {
        AppDebugLog.d(AppDebugLog.TAG_NET, "结束监听时间闹钟! isNull = " + (mOpenSpyIntent == null));
        if (mOpenSpyIntent != null) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(mOpenSpyIntent);
            mOpenSpyIntent = null;
        }
    }

    public long getLastOpenSpyTime() {
        return mLastOpenRecordTime;
    }
}
