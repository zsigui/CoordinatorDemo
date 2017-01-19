package com.luna.powersaver.gp.utils;

import com.luna.powersaver.gp.entity.JsonAppInfo;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by zsigui on 17-1-19.
 */

public class DBUtil {

    public static JsonAppInfo findInfoByPkg(String pkg) {
        return null;
    }

    public static boolean isContains(String pkg) {
        return true;
    }

    /**
     * 获取最后一次正在执行的任务，继续操作
     */
    public static JsonAppInfo getWorkingInfo() {
        return null;
    }

    public static void setWorkingInfo(JsonAppInfo info) {

    }

    public static HashMap<String, JsonAppInfo> getAllInfo() {
        return new HashMap<>();
    }

    public static void setAllInfo(HashMap<String, JsonAppInfo> infos) {

    }

    public static ConcurrentLinkedQueue<JsonAppInfo> getAllWaitingInfo() {
        // 获取所有待执行任务
        return new ConcurrentLinkedQueue<>();
    }

    public static boolean addNewInfo(JsonAppInfo info) {
        JsonAppInfo old = findInfoByPkg(info.pkg);
        if (old != null) {
            if (old.execState != JsonAppInfo.EXC_STATE.FINISHED
                    || (old.endtime < info.starttime)) {
                // 任务没完成或者需要重置
                info.execState = old.execState;
                // 进行更新
                updateInfo(info);
                return true;
            }
            // 任务已经完成，新增失败
            return false;
        }
        addNewInfoInternal(info);
        return true;
    }

    private static void updateInfo(JsonAppInfo info) {
        if (isContains(info.pkg)) {
            // 执行更新
        }
    }

    private static void addNewInfoInternal(JsonAppInfo info) {
        // 执行插入
    }
}
