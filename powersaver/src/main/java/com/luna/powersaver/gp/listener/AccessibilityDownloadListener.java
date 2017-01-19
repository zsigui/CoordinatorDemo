package com.luna.powersaver.gp.listener;

import com.luna.powersaver.gp.http.bean.DownloadInfo;
import com.luna.powersaver.gp.manager.StalkerManager;

/**
 * Created by zsigui on 17-1-18.
 */

public class AccessibilityDownloadListener implements DownloadInfo.DownloadListener {

    @Override
    public void onProgressUpdate(DownloadInfo info, int elapsedTime) {

    }

    @Override
    public void onFinishDownload(DownloadInfo info) {
        StalkerManager.get().fromDownloadingToDownloaded(info.getPackageName());
        // 执行安装任务
    }

    @Override
    public void onFailDownload(DownloadInfo info, String err) {

    }
}
