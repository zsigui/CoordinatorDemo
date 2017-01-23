package com.luna.powersaver.gp.common;

/**
 * Created by zsigui on 17-1-13.
 */

public class GPResId {

    public static final String PACKAGE = "com.android.vending";
    public static final String NOTIFY_DOWNLOAD_PKG = "com.android.providers.downloads";
    public static final String NOTIFY_GOOGLE_DOWNLOAD_PKG = "com.google.android.providers.downloads";
    public static final String INSTALLER_PKG = "com.android.packageinstaller";
    public static final String INSTALLER_GOOGLE_PKG = "com.google.android.packageinstaller";
    public static final String SETTINGS_PKG = "com.android.settings";
    public static final String ANDROID_PKG = "android";

    /*-------------------------- 以下部分为 Google Play Store 资源ID ----------------------------*/

    public static String getErrorRetryResId() { return PACKAGE + ":id/retry_button"; }
    /**
     * 应用首页标题栏
     * @return
     */
    public static String getMainBarResId() { return PACKAGE + ":id/play_header_list_tab_container"; }
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

    public static String getUninstallBtnId() { return PACKAGE + ":id/uninstall_button"; }

    public static String getLaunchBtnId() { return PACKAGE + ":id/launch_button"; }

    public static String getMainActivityClassName() {
        return "com.android.vending.AssetBrowserActivity";
    }

    public static String getWakeAction() {
        return "market://details?id=";
    }

    public static String getSearchIdleId() {
        return PACKAGE + ":id/search_box_idle_text";
    }

    public static String getSearchInputId() {
        return PACKAGE + ":id/search_box_text_input";
    }

    public static String getSearchSuggestionId() {
        return PACKAGE + ":id/play_search_suggestions_list";
    }

    public static String getSearchSuggestItemTextId() {
        return PACKAGE + ":id/suggest_text";
    }

    public static String getSearchResultListId() {
        return PACKAGE + ":id/search_results_list";
    }

    public static String getSearchResultListItemTitleId() {
        return PACKAGE + ":id/li_title";
    }

    public static String getSearchResultListItemSubTitleId() {
        return PACKAGE + ":id/li_subtitle";
    }

    /*-------------------------- 以下部分为 Installer 安装器资源ID ----------------------------*/

    public static String getInstallTitleId() {
        return INSTALLER_PKG + ":id/app_name";
    }

    public static String getInstallOkBtnId() {
        return INSTALLER_PKG + ":id/ok_button";
    }

    public static String getInstallCancelBtnId() {
        return INSTALLER_PKG + ":id/cancel_button";
    }

    public static String getInstalledLaunchBtnId() {
        return INSTALLER_PKG + ":id/launch_button";
    }

    public static String getInstalledDoneBtnId() {
        return INSTALLER_PKG + ":id/done_button";
    }

    public static String getUninstallTitleId() {
        return INSTALLER_PKG + ":id/alertTitle";
    }

    public static String getUninstallOkBtnId() {
        return INSTALLER_PKG + ":id/button1";
    }

    public static String getUninstallCancelBtnId() {
        return INSTALLER_PKG + ":id/button2";
    }

    /*------------ 系统弹窗 --------------*/
    public static String getSystemDialogTitleId(){ return  ANDROID_PKG + ":id/alertTitle"; }

    public static String getSystemDialogOkBtnId() { return ANDROID_PKG + ":id/button1"; }

    /*-------------- 设置 -----------*/
    public static String getSettingItemTitleId() { return ANDROID_PKG + ":id/title"; }

    public static String getSettingListViewId() { return ANDROID_PKG + ":id/listview"; }

    public static String getDetailSettingHeaderId() { return SETTINGS_PKG + ":id/all_details"; }

    public static String getDetailSettingPermissionSwitchId() { return INSTALLER_PKG + ":id/switchWidget"; }
}
