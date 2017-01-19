package com.luna.powersaver.gp.entity;

/**
 * Created by zsigui on 17-1-17.
 */

public class JsonResponse<T> {

    // 状态码
    public int c;

    // 错误码
    public String msg;

    // 结果
    public T d;
}
