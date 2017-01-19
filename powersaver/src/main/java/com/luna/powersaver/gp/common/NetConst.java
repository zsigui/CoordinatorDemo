package com.luna.powersaver.gp.common;

import com.luna.powersaver.gp.BuildConfig;

/**
 * Created by zsigui on 17-1-18.
 */

public final class NetConst {

    /*------------------ 请求信息 ---------------------*/

    private static String HOST = "";

    private static String TEST_HOST = "http://172.16.7.187:8000/sl/";

    public static String getHost() {
        return BuildConfig.IS_DEBUG ? TEST_HOST : HOST;
    }

    /*------------------ 状态码 -----------------------*/

    public static int STATUS_OK = 0;

    public static int STATUS_ERROR = -1;

    public static String STR_ERROR = "解析出错";
}
