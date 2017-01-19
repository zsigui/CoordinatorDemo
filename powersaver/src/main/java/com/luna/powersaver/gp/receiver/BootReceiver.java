package com.luna.powersaver.gp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import com.luna.powersaver.gp.async.NetAsyncTask;
import com.luna.powersaver.gp.http.DownloadManager;
import com.luna.powersaver.gp.manager.ClockManager;
import com.luna.powersaver.gp.service.GuardService;
import com.luna.powersaver.gp.service.NBAccessibilityService;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.NetworkUtil;

import static com.luna.powersaver.gp.manager.ClockManager.ACTION_CLOCK;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2017/1/12
 */

public class BootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || TextUtils.isEmpty(intent.getAction()))
            return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // 判断并启动服务
            GuardService.testAliveAndCreateIfNot(context);

        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {

            // 接收到网络连接状态变化的广播
            if (NBAccessibilityService.sIsInWork) {
                if (NetworkUtil.isWifiConnected(context)) {
                    DownloadManager.getInstance(context).startDownload();
                } else {
                    DownloadManager.getInstance(context).stopAllDownload();
                }
            }
        } else if (ClockManager.ACTION_CLOCK.equals(action)) {
            AppDebugLog.d(AppDebugLog.TAG_NET, "接收到轮询的广播请求! + " + ACTION_CLOCK);
            ClockManager.get().startAlarm(context);
            new NetAsyncTask().execute();
        }
    }
}
