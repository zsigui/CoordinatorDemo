package com.luna.powersaver.gp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.luna.powersaver.gp.manager.StalkerManager;
import com.luna.powersaver.gp.utils.AppDebugLog;

/**
 * Created by zsigui on 17-1-17.
 */

public class PackageInstallReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null)
            return;

        String action = intent.getAction();
        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "接收到卸载安装广播: " + action);
        String pkgName = intent.getData().getSchemeSpecificPart();
        if (StalkerManager.get().pCurrentWorkInfo != null
                && pkgName.equals(StalkerManager.get().pCurrentWorkInfo.pkg)) {
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                StalkerManager.get().doContinueAfterInstalled();
            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                StalkerManager.get().doContinueAfterUninstall();
            } else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                // ignored
            }
        }
    }
}
