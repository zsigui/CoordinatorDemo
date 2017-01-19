package com.luna.powersaver.gp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.luna.powersaver.gp.async.NetAsyncTask;
import com.luna.powersaver.gp.manager.ClockManager;
import com.luna.powersaver.gp.utils.AppDebugLog;

import static com.luna.powersaver.gp.manager.ClockManager.ACTION_CLOCK;

/**
 * Created by zsigui on 17-1-18.
 */

public class ClockReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        AppDebugLog.d(AppDebugLog.TAG_NET, "1接收到轮询的广播请求! + " + ACTION_CLOCK);
        if (context == null || intent == null)
            return;
        if (ACTION_CLOCK.equals(intent.getAction())) {
            ClockManager.get().startAlarm(context);
            new NetAsyncTask().execute();
        }
    }
}
