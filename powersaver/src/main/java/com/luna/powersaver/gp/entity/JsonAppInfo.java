package com.luna.powersaver.gp.entity;

/**
 * Created by zsigui on 17-1-17.
 */

public class JsonAppInfo {

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

    public interface KEEP_STATE {
        int NOT_WORK = 0;
        int UNINSTALL_INSTANT = 1;
        int UNINSTALL_AFTER_OPEN = 2;
        int NOT_WORK_AFTER_OPEN = 3;
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

    // 以下值暂时为自己添加，仅当task=3时必须， 3 GP搜索下载
    // 搜索键值
    public String searchkey;
    // 应用名称，用于匹配搜索结果
    public String appname;
    public int keepstate;
    // 开启时间，单位(s)
    public int opentime;

    public int execState = EXC_STATE.WAIT_TO_DOWNLOAD;
}
