package com.luna.powersaver.gp.http;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;

import com.luna.powersaver.gp.http.bean.DownloadInfo;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.FileUtil;
import com.luna.powersaver.gp.utils.chiper.MD5;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zsigui on 16-4-26.
 */
public class DownloadThread extends Thread implements DownloadInfo.DownloadListener {


    private static final int INTERVAL_TIME = 1000;
    private boolean mIsStop = false;
    private String mTag;
    private String mDirPath;
    private Context mContext;
    private LinkedBlockingQueue<DownloadInfo> mWaitQueue;

    public DownloadThread(Context context, String dirPath, LinkedBlockingQueue<DownloadInfo> waitQueue) {
        mContext = context;
        this.mWaitQueue = waitQueue;
        mDirPath = dirPath;
    }

    public void setIsStop(boolean isStop) {
        mIsStop = isStop;
    }

    public boolean isStop() {
        return mIsStop;
    }

    public void setTag(String tag) {
        mTag = tag;
    }

    private boolean initRange(DownloadInfo info) throws IOException {
        boolean result;
        URL url = new URL(info.getDownloadUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setRequestMethod("GET");
        // 默认HttpURLConnection会进行Gzip压缩，这时无法通过getContentLength获取长度，所以要禁掉这个
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.connect();
        final int code = connection.getResponseCode();
        if (code >= 200 && code < 300) {
            info.setTotalSize(connection.getContentLength());
        }
        connection.disconnect();
        //				RandomAccessFile accessFile = new RandomAccessFile(info.getTempFileName(), "rwd");
//				accessFile.setLength(info.getTotalSize());
//				accessFile.close();

        result = info.getTotalSize() > 0;
        if (result && info.getTotalSize() < info.getDownloadSize()) {
            info.setDownloadSize(0);
        }
        return result;
    }

    @Override
    public void run() {
        mIsStop = false;
        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, String.format(Locale.CHINA,
                "线程%s开始执行。。。。", mTag));
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        DownloadInfo info = null;
        while (!mIsStop) {
            try {
                AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, String.format(Locale.CHINA,
                        "线程%s进入获取任务流程，当前任务剩余数量:%d，执行状态: %b",
                        mTag, mWaitQueue.size(), mIsStop));
                if (mWaitQueue.size() == 0) {
                    // 列表已经无任务了，退出
                    AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, String.format("线程%s无任务，退出执行", mTag));
                    mIsStop = true;
                    DownloadManager.getInstance(mContext).judgeIsRunning();
                    return;
                }
                info = mWaitQueue.take();
                AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, String.format("线程%s执行下载任务：%s", mTag, info
                        .getDownloadUrl()));
                if (initRange(info)) {
                    if (info.isDownload()) {
                        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, String.format("下载任务：%s， 初始化完毕！", info
                                .getDownloadUrl()));
                        URL url = new URL(info.getDownloadUrl());
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        if (info.getTotalSize() > info.getDownloadSize()) {
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                connection.setFixedLengthStreamingMode(info.getTotalSize() - info.getDownloadSize());
                            } else {
                                connection.setFixedLengthStreamingMode((int) (info.getTotalSize() - info
                                        .getDownloadSize
                                                ()));

                            }
                        } else {
                            connection.setChunkedStreamingMode((int) info.getTotalSize());
                        }

                        // 默认HttpURLConnection会进行Gzip压缩，这时无法通过getContentLength获取长度，所以要禁掉这个
                        connection.setRequestProperty("Accept-Encoding", "identity");
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(DownloadManager.NET_CONNECT_TIMEOUT);
                        connection.setReadTimeout(DownloadManager.NET_READ_TIMEOUT);
                        if (info.getTotalSize() > 0) {
                            connection.setRequestProperty("Range", "bytes=" + info.getDownloadSize() + "-"
                                    + (info.getTotalSize() - 1));
                        } else {
                            connection.setRequestProperty("Range", "bytes=" + info.getDownloadSize() + "-");
                        }
                        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "下载开始：code = " + connection.getResponseCode() +
                                ":" +
                                connection.getResponseMessage());

                        int code = connection.getResponseCode();
                        if (code >= 200 && code < 300) {
                            InputStream in = connection.getInputStream();
                            BufferedInputStream bin = new BufferedInputStream(in);
                            File tempFile = new File(mDirPath, info.getTempFileName());
                            RandomAccessFile out = new RandomAccessFile(tempFile, "rwd");
                            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "start to download: store file path = "
                                    + tempFile.getAbsolutePath());
                            out.seek(info.getDownloadSize());
                            int length;
                            byte[] bs = new byte[4096];
                            long startTime = System.currentTimeMillis();
                            long stopTime;
                            while ((length = bin.read(bs)) != -1) {
                                out.write(bs, 0, length);
                                info.setDownloadSize(info.getDownloadSize() + length);
                                stopTime = System.currentTimeMillis();
                                if (stopTime - startTime >= INTERVAL_TIME) {
                                    onProgressUpdate(info, INTERVAL_TIME);
                                    startTime = stopTime;
                                }
                                if (!info.isDownload()) {
                                    // 暂停下载
                                    AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, tempFile.getName() + " 下载暂停");
                                    break;
                                }
                            }
                            out.close();
                            if (info.isDownload() && length == -1) {
                                // 下载完成
                                if (!TextUtils.isEmpty(info.getMd5Sum())) {
                                    if (MD5.checkMd5Sum(tempFile, info.getMd5Sum())) {
                                        // 验证不通过，下载的包有问题，需要重新下载
                                        failDownload(info, true);
                                        onFailDownload(info, "MD5验证失败");
                                        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, String.format("url : %s 执行验证，验证MD5不通过！",
                                                info.getDownloadUrl()));
                                    } else {
                                        finishDownload(info, tempFile);
                                        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD,
                                                String.format("url : %s 执行验证，验证MD5通过，下载完成！",
                                                        info.getDownloadUrl()));
                                    }
                                } else {
                                    finishDownload(info, tempFile);
                                    AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, String.format("url : %s 不执行验证，下载完成！",
                                            info.getDownloadUrl()));
                                }
                                continue;
                            }
                        }
                        // 取消下载
//                        SilentDownloadManager.getInstance().removeDownload(info, false);
                        onFailDownload(info, "取消下载");
                        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, String.format("url : %s 取消下载！", info.getDownloadUrl
                                ()));

                    } else {
                        // 连接错误，下载失败，将任务重新移动队列末尾
                        failDownload(info, false);
                        onFailDownload(info, "连接出错");
                        AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, String.format("url : %s 下载失败！", info.getDownloadUrl()));
                    }
                } else {
                    onFailDownload(info, "初始化文件信息失败");
                    AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "初始化文件信息失败");
                }

            } catch (IOException | InterruptedException e) {
                AppDebugLog.w(AppDebugLog.TAG_DOWNLOAD, e);
                if (info != null) {
                    failDownload(info, false);
                }
            }
        }
    }

    private void failDownload(DownloadInfo info, boolean removeTemp) {
        if (info.getRetryTime() <= 0) {
            return;
        }
        info.setRetryTime(info.getRetryTime() - 1);
        DownloadManager.getInstance(mContext).removeDownload(info, removeTemp);
        DownloadManager.getInstance(mContext).addDownload(info);
    }

    private void finishDownload(DownloadInfo info, File tempFile) {
        onFinishDownload(info);
        FileUtil.copy(tempFile, new File(mDirPath, info.getStoreFileName()));
        DownloadManager.getInstance(mContext).removeDownload(info, true);
    }

    @Override
    public void onProgressUpdate(DownloadInfo info, int elapsedTime) {
        DownloadManager.getInstance(mContext).onProgressUpdate(info, INTERVAL_TIME);
        if (info != null && info.getListener() != null) {
            info.getListener().onProgressUpdate(info, elapsedTime);
        }
    }

    @Override
    public void onFinishDownload(DownloadInfo info) {
        DownloadManager.getInstance(mContext).onFinishDownload(info);
        if (info != null && info.getListener() != null) {
            info.getListener().onFinishDownload(info);
        }
    }

    @Override
    public void onFailDownload(DownloadInfo info, String err) {
        DownloadManager.getInstance(mContext).onFailDownload(info, err);
        if (info != null && info.getListener() != null) {
            info.getListener().onFailDownload(info, err);
        }
    }
}
