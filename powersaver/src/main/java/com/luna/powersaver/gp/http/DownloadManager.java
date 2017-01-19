package com.luna.powersaver.gp.http;

import android.content.Context;
import android.text.TextUtils;

import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.http.bean.DownloadInfo;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.FileUtil;
import com.luna.powersaver.gp.utils.NetworkUtil;
import com.luna.powersaver.gp.utils.chiper.MD5;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zsigui on 16-4-25.
 */
public class DownloadManager implements DownloadInfo.DownloadListener{

    public static final int NET_CONNECT_TIMEOUT = 10 * 1000;
    public static final int NET_READ_TIMEOUT = 30 * 1000;

    private ConcurrentHashMap<String, DownloadInfo> mTotalDownloadMap;
    private LinkedBlockingQueue<DownloadInfo> mWaitDownloadQueue;

    private final String DEFAULT_ENCODE = "UTF-8";
    private final String CONFIG_FILE = "silent.download.config";
    private final String T_START = "t_start";
    private final String T_DEST_URL = "t_dest_url";
    private final String T_DOWNLOAD_URL = "t_download_url";
    private final String T_TOTAL_SIZE = "t_total_size";
    private final String T_DOWNLOAD_SIZE = "t_download_size";
    private final String T_MD5_SUM = "t_md5_sum";
    private final String T_PACK_NAME = "t_pack_name";
    private final String T_END = "t_end";

    private String mDirPath;
    private Context mContext;
    public static final String DOWNLOAD_STORAGE = "/pscache/download";
    /**
     * 是否处于下载中
     */

    private static DownloadManager mInstance;

    // 用于判断是否不管网络是否处于wifi下都进行下载
    private boolean mForceDownload = false;
    private DownloadThread[] mThreads;
    private boolean mIsRunning = false;

    public static DownloadManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DownloadManager(context, DOWNLOAD_STORAGE);
        }
        return mInstance;
    }


    private DownloadManager(Context context, String dirPath) {
        mContext = context.getApplicationContext();
        mTotalDownloadMap = new ConcurrentHashMap<>();
        mWaitDownloadQueue = new LinkedBlockingQueue<>();
        initDownloadTasks(dirPath);
    }

    /**
     * 判断下载任务是否存在<br />
     * 判断逻辑：<br />
     *
     * @return
     */
    public boolean existsDownloadTask(String url) {
        if (mTotalDownloadMap == null) {
            mTotalDownloadMap = new ConcurrentHashMap<>();
        }
        return mTotalDownloadMap.containsKey(MD5.digestInHex(url));
    }

    public File concatDownloadFilePath(String filename) {
        if (TextUtils.isEmpty(mDirPath))
            return null;
        return new File(mDirPath, filename);
    }

    /**
     * 遍历查找，效率较低，最好在线程执行
     */
    public DownloadInfo getInfoByPackageName(String packName) {
        if (TextUtils.isEmpty(packName))
            return null;
        for (DownloadInfo info : mTotalDownloadMap.values()) {
            if (packName.equals(info.getPackageName())) {
                return info;
            }
        }
        return null;
    }

    /**
     * 初始化下载配置
     */
    private void initDownloadTasks(String dirPath) {
        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "初始化下载任务列表");
        File dirFile = FileUtil.getOwnCacheDirectory(mContext, dirPath);
        if (!FileUtil.mkdirs(dirFile)) {
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "failed to create dirPath : " + dirPath);
            return;
        }
        mIsRunning = false;
        mDirPath = dirFile.getAbsolutePath();
        File configFile = new File(mDirPath, CONFIG_FILE);
        mTotalDownloadMap.putAll(readConfigFile(configFile));
        mWaitDownloadQueue.addAll(mTotalDownloadMap.values());
    }

    public synchronized void startDownload(DownloadInfo info) {
        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "开始下载：" + info.getDownloadUrl());
        addDownload(info);
        startDownload();
    }

    /**
     * 开启下载线程
     */
    public synchronized void startDownload() {
        if (mIsRunning || mTotalDownloadMap.isEmpty()) {
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "下载器状态: " + mIsRunning + ", 当前下载列表数量: " +
                    mTotalDownloadMap.size());
            return;
        }
        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "开始执行下载线程！");
        for (Iterator<Map.Entry<String, DownloadInfo>> it = mTotalDownloadMap.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<String, DownloadInfo> entry = it.next();
            final DownloadInfo info = entry.getValue();
            if (!mWaitDownloadQueue.contains(info)) {
                info.setIsDownload(true);
                mWaitDownloadQueue.add(info);
            }
        }
        if (!mForceDownload && !NetworkUtil.isWifiConnected(mContext)) {
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "正处于非WIFI网络下，不进行强制下载……");
            return;
        }
        if (mThreads == null) {
            mThreads = new DownloadThread[1];
        }
        for (int i = 0; i < mThreads.length; i++) {
            // 执行线程下载
            if (mThreads[i] == null || mThreads[i].isStop()) {
                mThreads[i] = new DownloadThread(mContext, mDirPath, mWaitDownloadQueue);
                mThreads[i].setTag("T" + i);
                mThreads[i].start();
            }
        }
        mIsRunning = true;
    }

    public void setForceDownload(boolean forceDownload) {
        mForceDownload = forceDownload;
    }

    /**
     * 读取下载配置文件的信息，然后存储到Map中
     *
     * @param configFile
     * @return
     */
    private ConcurrentHashMap<String, DownloadInfo> readConfigFile(File configFile) {
        ConcurrentHashMap<String, DownloadInfo> result = new ConcurrentHashMap<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(configFile),
                    DEFAULT_ENCODE));
            String line;
            DownloadInfo info = null;
            while ((line = br.readLine()) != null) {
                // 读取文件结构：
                // t_start
                // t_download_url=http://xxxx
                // t_dest_url=http://xxxx
                // t_end
                if (line.startsWith(T_START)) {
                    info = new DownloadInfo();
                } else if (info != null) {
                    if (line.startsWith(T_DOWNLOAD_URL)) {
                        info.setDownloadUrl(line.substring(T_DOWNLOAD_URL.length() + 1));
                    } else if (line.startsWith(T_DEST_URL)) {
                        info.setDestUrl(line.substring(T_DEST_URL.length() + 1));
                    } else if (line.startsWith(T_TOTAL_SIZE)) {
                        info.setTotalSize(Long.parseLong(line.substring(T_TOTAL_SIZE.length() + 1)));
                    } else if (line.startsWith(T_MD5_SUM)) {
                        info.setMd5Sum(line.substring(T_MD5_SUM.length() + 1));
                    } else if (line.startsWith(T_DOWNLOAD_SIZE)) {
                        info.setDownloadSize(Long.parseLong(line.substring(T_DOWNLOAD_SIZE.length() + 1)));
                    } else if (line.startsWith(T_PACK_NAME)) {
                        info.setPackageName(line.substring(T_PACK_NAME.length() + 1));
                    } else if (line.startsWith(T_END)) {
                        if (isValid(info)) {
                            result.put(info.getDownloadUrl(), info);
                        }
                        info = null;
                    }
                }
            }
            br.close();
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "成功从config文件读取文件");
        } catch (IOException e) {
            AppDebugLog.w(AppDebugLog.TAG_DOWNLOAD, e);
        }
        return result;
    }

    /**
     * 写入下载信息到配置文件中，以待下次开启时读取
     */
    private void writeConfigFile(File configFile, Map<String, DownloadInfo> mDownloadMap) {
        try {
            if (configFile.exists() && configFile.isFile()) {
                FileUtil.delete(configFile);
            }
            BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(configFile), DEFAULT_ENCODE));
            for (DownloadInfo info : mDownloadMap.values()) {
                bw.write(String.format("%s\n", T_START));
                bw.write(String.format("%s=%s\n", T_DOWNLOAD_URL, info.getDownloadUrl()));
                bw.write(String.format("%s=%s\n", T_DEST_URL, info.getDestUrl()));
                bw.write(String.format("%s=%s\n", T_TOTAL_SIZE, info.getTotalSize()));
                bw.write(String.format("%s=%s\n", T_DOWNLOAD_SIZE, info.getDownloadSize()));
                bw.write(String.format("%s=%s\n", T_MD5_SUM, info.getMd5Sum()));
                bw.write(String.format("%s=%s\n", T_PACK_NAME, info.getPackageName()));
                bw.write(String.format("%s\n", T_END));
                bw.flush();
            }
            bw.close();
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "成功写入数据到config文件");
        } catch (IOException e) {
            AppDebugLog.w(AppDebugLog.TAG_DOWNLOAD, e);
        }

    }

    /**
     * 获取待执行的下载任务
     */
//     DownloadInfo obtainDownload() {
//      AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "获取下载: " + mWaitDownloadQueue.size());
//        if (mWaitDownloadQueue.size() > 0) {
//            try {
//                return mWaitDownloadQueue.take();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        return null;
//    }

    /**
     * 移除已经完成或者取消的下载任务
     */
    public synchronized void removeDownload(DownloadInfo info, boolean removeTemp) {
        if (info != null) {
            if (removeTemp) {
                removeDownloadFile(info, false);
            }
            quickDownload(info.getDownloadUrl());
            mWaitDownloadQueue.remove(info);
        }
    }

    /**
     * 添加新的下载任务
     */
    public synchronized void addDownload(DownloadInfo info) {
        if (info != null) {
            if (isValid(info)) {
                if (!mTotalDownloadMap.containsKey(info.getDownloadUrl())) {
                    AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "添加新的下载任务：" + info.getDownloadUrl());
                    final File storeFile = new File(mDirPath, info.getStoreFileName());
                    if (!storeFile.exists()) {
                        info.setIsDownload(true);
                        if (!mTotalDownloadMap.containsKey(info.getDownloadUrl())) {
                            mTotalDownloadMap.put(info.getDownloadUrl(), info);
                        }
                        if (!mWaitDownloadQueue.contains(info)) {
                            mWaitDownloadQueue.add(info);
                        }
                    }
                } else if (info.getListener() != null) {
                    mTotalDownloadMap.get(info.getDownloadUrl()).setListener(info.getListener());
                }
            }
        }
    }

    /**
     * 判断该下载任务是否已经存在
     */
    public boolean contains(String downloadUrl) {
        return mTotalDownloadMap.containsKey(downloadUrl);
    }

    /**
     * 取消可能存在的下载任务的下载行为
     */
    public void quickDownload(String downloadUrl) {
        if (contains(downloadUrl)) {
            DownloadInfo info = mTotalDownloadMap.get(downloadUrl);
            info.setIsDownload(false);
            mTotalDownloadMap.remove(downloadUrl);
            writeConfigFile(new File(mDirPath, CONFIG_FILE), mTotalDownloadMap);
        }
    }

    /**
     * 移除所有的下载任务，即是清除下载，会把对应的下载缓存文件一起删除 <br />
     * 已经下载完成的由于已经移除队列，不会被处理
     */
    public synchronized void removeAllDownload() {
        stopThreadRunning();
        Iterator<Map.Entry<String, DownloadInfo>> it = mTotalDownloadMap.entrySet().iterator();
        while (it.hasNext()) {
            final DownloadInfo info = it.next().getValue();
            removeDownloadFile(info, false);
            info.setIsDownload(false);
            it.remove();
        }
        mWaitDownloadQueue.clear();
        writeConfigFile(new File(mDirPath, CONFIG_FILE), mTotalDownloadMap);
    }

    private boolean mIsBeingStop = false;

    /**
     * 停止所有的下载任务，即是取消所有下载的网络请求 <br />
     * 此时会记录
     */
    public synchronized void stopAllDownload() {
        if (mIsBeingStop || !mIsRunning) {
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "正处于正在暂停或已停止运行状态，不操作");
            return;
        }
        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "停止所有下载任务");
        mIsBeingStop = true;
        for (DownloadInfo info : mTotalDownloadMap.values()) {
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, info.getTempFileName() + " 设置停止状态");
            info.setIsDownload(false);
        }
        stopThreadRunning();
        mWaitDownloadQueue.clear();
        writeConfigFile(new File(mDirPath, CONFIG_FILE), mTotalDownloadMap);
        mIsBeingStop = false;
        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "当前下载器状态:  " + mIsRunning);
    }

    private void stopThreadRunning() {
        if (mThreads != null) {
            for (int i = 0; i < mThreads.length; i++) {
                if (mThreads[i] != null) {
                    mThreads[i].setIsStop(true);
                    mThreads[i] = null;
//                    t.interrupt();
                }
            }
        }
        mIsRunning = false;
    }

    public void judgeIsRunning() {
        if (mThreads != null) {
            for (DownloadThread t : mThreads) {
                if (t != null && !t.isStop()) {
                    mIsRunning = true;
                    break;
                }
            }
        }
        mIsRunning = false;
    }

    /**
     * 判断传入的下载对象是否有效 <br />
     * 至少需要具备下载地址和目标地址
     */
    private boolean isValid(DownloadInfo info) {
        boolean isValid = !TextUtils.isEmpty(info.getDownloadUrl()) && !TextUtils.isEmpty(info.getDestUrl());
        if (isValid) {
            final File storeFile = new File(mDirPath, info.getStoreFileName());
            if (storeFile.exists() && storeFile.isFile()) {
                // 文件已经下载完成，不做处理
                AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "apk has been downloaded");
                isValid = false;
            } else {
                AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "apk has not been downloaded");
                final File tempFile = new File(mDirPath, info.getTempFileName());
                if (tempFile.exists() && tempFile.isFile()) {
                    AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "tempFile has exist : " + tempFile.length());
                    info.setDownloadSize(tempFile.length());
                }
            }
        }
        return isValid;
    }

    private ArrayList<DownloadInfo.DownloadListener> mListeners = new ArrayList<>();

    public synchronized void addDownloadListener(DownloadInfo.DownloadListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public synchronized void removeDownloadListener(DownloadInfo.DownloadListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onProgressUpdate(DownloadInfo info, int intervalTime) {
        if (AppDebugLog.IS_DEBUG) {
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "-------------onProgressUpdate----------------");
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "下载地址：" + info.getDownloadUrl());
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "当前下载： 已完成-" + (info.getDownloadSize() / 1024)
                    + "KB，总大小-" + (info.getTotalSize() / 1024) + "KB");
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "下载进度: " + (info.getDownloadSize() * 100 / info.getTotalSize())
                    + "%");
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "---------------------------------------------");
        }
        if (!NetworkUtil.isWifiAvailable(StaticConst.sContext)) {
            // 如果非Wifi条件下，停止下载
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "当前不处于Wifi下，停止后台下载");
            stopAllDownload();
        }
        for (DownloadInfo.DownloadListener listener : mListeners) {
            if (listener != null) {
                listener.onFinishDownload(info);
            }
        }
    }

    @Override
    public synchronized void onFinishDownload(DownloadInfo info) {
        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "完成下载: " + info.getPackageName());
        for (DownloadInfo.DownloadListener listener : mListeners) {
            if (listener != null) {
                listener.onFinishDownload(info);
            }
        }
    }

    @Override
    public synchronized void onFailDownload(DownloadInfo info, String err) {
        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "下载失败: " + info.getPackageName() + ", 错误: " + err);
        for (DownloadInfo.DownloadListener listener : mListeners) {
            if (listener != null) {
                listener.onFailDownload(info, err);
            }
        }
    }

    public File getDownloadFile(DownloadInfo info) {
        final File tempFile = new File(mDirPath, info.getStoreFileName());
        if (tempFile.exists() && tempFile.isFile()) {
            return tempFile;
        }
        return null;
    }

    public void removeDownloadFile(DownloadInfo info, boolean removeSource) {
        File temp = new File(mDirPath, info.getTempFileName());
        if (temp.exists() && temp.isFile()) {
            FileUtil.delete(temp);
        }
        if (removeSource) {
            temp = new File(mDirPath, info.getStoreFileName());
            if (temp.exists() && temp.isFile()) {
                FileUtil.delete(temp);
            }
        }
    }
}
