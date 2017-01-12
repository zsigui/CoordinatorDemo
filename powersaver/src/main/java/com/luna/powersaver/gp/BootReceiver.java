package com.luna.powersaver.gp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2017/1/12
 */

public class BootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null)
            return;
        Log.d("boot-test", "BootReceiver receive!");
        GuardService.testAliveAndCreateIfNot(context);
    }
}
