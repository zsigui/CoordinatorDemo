package com.luna.powersaver.gp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import com.luna.powersaver.gp.PowerSaver;
import com.luna.powersaver.gp.async.NetAsyncTask;
import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.entity.JsonAppInfo;
import com.luna.powersaver.gp.http.DownloadManager;
import com.luna.powersaver.gp.manager.ClockManager;
import com.luna.powersaver.gp.manager.StalkerManager;
import com.luna.powersaver.gp.service.GuardService;
import com.luna.powersaver.gp.service.NBAccessibilityService;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.AppUtil;
import com.luna.powersaver.gp.utils.NetworkUtil;

import static com.luna.powersaver.gp.manager.ClockManager.ACTION_CLOCK;
import static com.luna.powersaver.gp.manager.ClockManager.ACTION_OPEN_SPY;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2017/1/12
 */

public class CReceiver extends BroadcastReceiver {
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
            if (NetworkUtil.isWifiConnected(context)) {
                if (NBAccessibilityService.sIsInWork) {
                    DownloadManager.getInstance(context).startDownload();
                }
            } else {
                DownloadManager.getInstance(context).stopAllDownload();
            }
        } else if (ClockManager.ACTION_CLOCK.equals(action)) {
            AppDebugLog.d(AppDebugLog.TAG_NET, "接收到轮询的广播请求! + " + ACTION_CLOCK);
            ClockManager.get().startAlarm(context);
            new NetAsyncTask().execute();
        } else if (ClockManager.ACTION_OPEN_SPY.equals(action)) {
            AppDebugLog.d(AppDebugLog.TAG_NET, "接收到打开监听的广播! + " + ACTION_OPEN_SPY);
            if (NBAccessibilityService.sIsInWork) {
                JsonAppInfo info = StalkerManager.get().pCurrentWorkInfo;
                if (info != null && AppUtil.isPkgForeground(StaticConst.sContext, info.pkg)) {
                    AppDebugLog.d(AppDebugLog.TAG_NET, "当前应用正在前台，进行累加，时间：" + info.openedtime);
                    info.openedtime += (System.currentTimeMillis() - ClockManager.get().getLastOpenSpyTime()) / 1000;
                    if (StalkerManager.get().isFinishedOpen()) {
//                        AppInfoUtil.jumpToHome(StaticConst.sContext);
                        StalkerManager.get().doContinueAfterOpened();
                    } else {
                        ClockManager.get().startOpenRecordAlarm(StaticConst.sContext);
                    }
                }
            }
        } else if ("android.hardware.usb.action.USB_STATE".equals(action)){
            // android.hardware.usb.action.USB_STATE
            // usb 接入的时候进行充电锁屏
            AppDebugLog.d(AppDebugLog.TAG_NET, "接收到打开监听的广播! + android.hardware.usb.action.USB_STATE ");
            boolean connected = intent.getBooleanExtra("connected", false);
            if (connected){
                PowerSaver.get().showGuardView(context);
            }
        }
    }
}
