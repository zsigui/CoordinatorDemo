package com.luna.powersaver.gp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.luna.powersaver.gp.http.bean.DownloadInfo;
import com.luna.powersaver.gp.manager.StalkerManager;

/**
 * Created by zsigui on 17-1-17.
 */

public class PackageInstallReceiver extends BroadcastReceiver{

    /**
     * 记录当前已经下载好启动安装的应用
     */
    public static DownloadInfo sDownloadToInstall;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null)
            return;

        String action = intent.getAction();

        String pkgName = intent.getData().getSchemeSpecificPart();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
//            if (sDownloadToInstall != null && StaticConst.sWaitToInstallMap.containsKey(pkgName)) {
//                StaticConst.sInstalledMap.put(pkgName, StaticConst.sWaitToInstallMap.remove(pkgName));
//                DownloadManager.getInstance(context).removeDownload(sDownloadToInstall, true);
//                sDownloadToInstall = null;
//            }
            StalkerManager.get().fromDownloadedToInstalled(pkgName);
//            AppInfoUtil.install(context, intent.hasFileDescriptors());
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            StalkerManager.get().fromInstalledToFinished(pkgName);
        }
//        else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
//        } else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
//        }
    }
}
