package com.jackiez.materialdemo.extra.service;

/**
 * Created by zsigui on 17-1-13.
 */

public class GPResId {

    public static final String PACKAGE = "com.android.vending";

    /**
     * 应用详情页：安装按钮
     * @return
     */
    public static String getInstallBtnResId() {
        return PACKAGE + ":id/buy_button";
    }

    /**
     * 应用详情页：年龄范围
     * @return
     */
    public static String getAgeRangeId() {
        return PACKAGE + ":id/title_content_rating_icon";
    }

    /**
     * 应用详情页：徽章ID
     * @return
     */
    public static String getDiscoveryBarId() {
        return PACKAGE + ":id/discovery_bar";
    }

    /**
     * 应用详情页：应用名称
     *
     * @return
     */
    public static String getTitleId() {
        return PACKAGE + ":id/title_title";
    }

    public static String getContinueBarId() {
        return PACKAGE + ":id/continue_button_bar";
    }

    public static String getContinueBtnId() {
        return PACKAGE +  ":id/continue_button";
    }

    public static String getMainActivityClassName() {
        return "com.google.android.finsky.activities.MainActivity";
    }

    public static String getWakeAction() {
        return "market://details?id=";
    }
}
