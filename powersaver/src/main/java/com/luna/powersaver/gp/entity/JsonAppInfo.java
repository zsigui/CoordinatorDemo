package com.luna.powersaver.gp.entity;

/**
 * Created by zsigui on 17-1-17.
 */

public class JsonAppInfo {

    /**
     * 默认至少需要打开的时间，单位 s
     */
//    public static final int DEFAULT_OPEN_TIME = 5 * 60;
    public static final int DEFAULT_OPEN_TIME = 30;

    public interface TASK {
        int DOWNLOAD_BY_GP = 1;
        int DOWNLOAD_BY_APK = 2;
    }

    public interface EXC_STATE {
        int WAIT_TO_DOWNLOAD = 0;
        // 以下状态都说明在执行中
        // 执行下载中
        int DOWNLOADING = 1;
        // 已经完成下载
        int DOWNLOADED = 2;
        // 已经安装
        int INSTALLED = 3;
        // 已经执行过打开操作
        int OPENED = 4;
        // 任务按时完成
        int FINISHED = 5;
        // 废弃，超过了任务时间段
        int DISCARD = 6;
    }

    public interface ACTION {
        /**
         * 下载后不操作
         */
        int NOT_WORK = 0;
        /**
         * 安装后不操作
         */

        int NOT_WORK_AFTER_INSTALL = 2;
        /**
         * 安装后卸载
         */
        int UNINSTALL_AFTER_INSTALL = 3;
        /**
         * 打开后不操作
         */
        int NOT_WORK_AFTER_OPEN = 4;
        /**
         * 打开后卸载
         */
        int UNINSTALL_AFTER_OPEN = 5;

    }

    // 任务类型 1 GP下载 2 APK下载
    public int task = TASK.DOWNLOAD_BY_GP;

    // 应用包名
    public String pkg;

    // 可选，APK下载地址，仅当task为2时必须
    public String url;

    // 该次任务完成时间段开始时间戳(s)
    public long starttime;

    // 该次任务完成时间段结束时间戳(s)
    public long endtime;

    //
    public String uri;

    // 行为模式
    public int action = ACTION.NOT_WORK;
    // 执行完毕后的操作 0 : 不操作(默认) 1 : 返回桌面  P.S.(这个有点奇怪，先忽略吧)
    public int endaction = 1;
    // 最少需要开启时间，单位(s)
    public int exp;
    // 已经开启时间，单位(s)
    public int openedtime = 0;

    public int execstate = EXC_STATE.WAIT_TO_DOWNLOAD;

    // 启动方式 0 Activity 1 Service 2 Broadcast
    public int start;

    // 以下值暂时为自己添加，仅当task=3时必须， 3 GP搜索下载
    // 搜索键值
    public String searchkey;
    // 应用名称，用于匹配搜索结果
    public String appname;

}
