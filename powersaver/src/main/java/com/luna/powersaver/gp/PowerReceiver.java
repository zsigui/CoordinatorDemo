package com.luna.powersaver.gp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.luna.powersaver.gp.utils.BatteryUtil;

/**
 * Created by zsigui on 17-1-9.
 */

public class PowerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null)
            return;

        GuardService.testAliveAndCreateIfNot(context);

        Log.d("ps-test", "PowerReceiver receive! = " + intent.getAction());
        if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            // 显示屏幕保护
            ViewManager.get().showGuard(context);
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
            // 关闭屏幕保护
            ViewManager.get().hideGuard(context);
            BatteryUtil.clearCalculateSetting();
        }
    }
}
