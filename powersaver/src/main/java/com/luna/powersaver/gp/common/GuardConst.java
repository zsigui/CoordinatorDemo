package com.luna.powersaver.gp.common;

/**
 * Created by zsigui on 17-1-13.
 */

public class GuardConst {

    public static final String SUPER_CLEANER = "com.turboc.cleaner";
    public static final String SYSTEM_UI = "com.android.systemui";

    public static final String ANDROID = "android";
    public static final String LAUNCHER_ONEPLUS_PN = "com.oneplus.hydrogen.launcher";
    public static final String LAUNCHER_SEC_PN = "com.sec.android.app.launcher";

    public static String getAlertTitle() {
        return ANDROID + ":id/alertTitle";
    }

    public static String getAlertMsg() {
        return ANDROID + ":id/message";
    }

    public static String getAlertOkBtn() {
        return ANDROID + ":id/button1";
    }

    public static String getAlertCancelBtn() {
        return ANDROID + ":id/button2";
    }

    public static String getDetailSettingTitle() { return ANDROID + ":id/title"; }

    public static String getDetailSettingTitle2() { return GPResId.SETTINGS_PKG + ":id/app_name"; }

    public static String getDetailSettingVersion() { return GPResId.SETTINGS_PKG + ":id/widget_text1"; }

    public static String getOnePlusAlertTitle() { return "com.hydrogen:id/alertTitle"; }

    public static String getSwitchWidgetId() { return GPResId.SETTINGS_PKG + ":id/switch_widget"; }
}
