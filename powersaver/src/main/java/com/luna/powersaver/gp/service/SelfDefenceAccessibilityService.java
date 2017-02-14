package com.luna.powersaver.gp.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.luna.powersaver.gp.R;
import com.luna.powersaver.gp.common.GPResId;
import com.luna.powersaver.gp.common.GuardConst;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.AppUtil;

import java.util.List;

/**
 * Created by zsigui on 17-2-14.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SelfDefenceAccessibilityService extends AccessibilityService {

    private String curInstallPn = GPResId.INSTALLER_PKG;
    private String appName;
    private int inPageState = PAGESTATE.DEFAULT;

    // 广告APK名称，如果多个或者由服务器获取，可以设置成列表
    private String shorfilmName = "com.videos.android.helper";

    public interface PAGESTATE {
        int DEFAULT = 0;
        int DETAIL_OR_GP = 1;
        int ACCESSIBILITY = 2;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (AppUtil.isPkgInstalled(getBaseContext(), GPResId.INSTALLER_PKG)) {
            curInstallPn = GPResId.INSTALLER_PKG;
        } else if (AppUtil.isPkgInstalled(getBaseContext(), GPResId.INSTALLER_GOOGLE_PKG)) {
            curInstallPn = GPResId.INSTALLER_GOOGLE_PKG;
        }

        try {
            PackageManager pm = getApplicationContext().getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(getPackageName(), 0);
            appName = (String) pm.getApplicationLabel(ai);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        handleSelfDefenceJudge(event);
    }

    /**
     * 进行防卸载保护
     * @param event
     */
    private void handleSelfDefenceJudge(AccessibilityEvent event) {
        AccessibilityNodeInfo source = getRootInActiveWindow();
        if (event == null || event.getPackageName() == null || source == null)
            return;
        String pkg = event.getPackageName().toString();

        AccessibilityNodeInfo info;
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "当前进行判断的包名：" + pkg);
                if (GPResId.PACKAGE.equals(pkg)) {
                    info = findViewByID(source, GPResId.getTitleId());
                    if (info != null) {
                        AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "查找到正处于GP详情页，判断是否本应用");
                        if (judgeTextContains(info, appName) || judgeTextContains(info, shorfilmName)) {
                            // 当前正处于本应用界面
                            AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "该详情页为本应用详情页，关注后续执行");
                            inPageState = PAGESTATE.DETAIL_OR_GP;
                        } else {
                            AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "该详情页不为本应用详情页");
                            inPageState = PAGESTATE.DEFAULT;
                        }
                        return;
                    }
                } else if (GPResId.SETTINGS_PKG.equals(pkg)) {
                    info = findViewByID(source, GuardConst.getDetailSettingTitle());
                    if (info != null) {
                        // 多加一层是因为避免无障碍处的影响
                        if (findViewByID(source, GuardConst.getDetailSettingVersion()) != null) {
                            AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "查找到正处于应用详情页，判断是否本应用");
                            if (judgeTextContains(info, appName) || judgeTextContains(info, shorfilmName)) {
                                // 当前正处于本应用界面
                                AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "该详情页为本应用详情页，关注后续执行");
                                inPageState = PAGESTATE.DETAIL_OR_GP;
                            } else {
                                AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "该详情页不为本应用详情页");
                                inPageState = PAGESTATE.DEFAULT;
                            }
                        }
                        return;
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "inPageState = " + inPageState + ", pkg = " + pkg);
                if (inPageState == PAGESTATE.DETAIL_OR_GP) {
                    if (GPResId.PACKAGE.equals(pkg)
                            || GPResId.SETTINGS_PKG.equals(pkg)) {
                        info = findViewByID(source, GuardConst.getAlertMsg());
                        AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "info = " + info);
                        if (judgeTextContains(info, getString(R.string.powersaver_uninstall_text))
                                || judgeTextContains(info, getString(R.string.powersaver_force_stop_text))) {
                            // 准备要卸载应用或者强制应用暂停
                            AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "本应用正准备执行卸载或者强制暂停操作，自动单击取消");
                            performViewClick(findViewByID(source, GuardConst.getAlertCancelBtn()));
                            return;
                        }
                    }
                } else if (inPageState == PAGESTATE.ACCESSIBILITY) {
                    info = findViewByID(source, GuardConst.getSwitchWidgetId());
                    if (info != null) {
                        // 表示进入无障碍的选项了
                        AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "本应用正处于可以关闭无障碍应用的界面，后退之");
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                        return;
                    }
                    info = findViewByID(source, GuardConst.getAlertMsg());
                    if (judgeTextContains(info, appName)) {
                        AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "本应用正处于可以关闭无障碍应用的界面，且触发了弹窗，后退之");
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                        return;
                    }
                }
                if (curInstallPn.equals(pkg)) {
                    AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "当前是处于installler状态，判断是否为弹窗且为本应用");
                    info = findViewByID(source, GuardConst.getAlertTitle());
                    if (judgeTextContains(info, appName) || judgeTextContains(info, shorfilmName)) {
                        AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "本应用执行安装卸载状态中，判断是否处于卸载");
                        info = findViewByID(source, GuardConst.getAlertMsg());
                        if (judgeTextContains(info, getString(R.string.powersaver_uninstall_text))) {
                            AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "本应用正准备执行卸载任务，自动单击取消");
                            performViewClick(findViewByID(source, GuardConst.getAlertCancelBtn()));
                            return;
                        }
                    }
                }
                if (GuardConst.LAUNCHER_ONEPLUS_PN.equals(pkg)) {
                    // 针对ONEPLUS
                    AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "当前是处于installler状态，判断是否为弹窗且为本应用(一加)");
                    info = findViewByID(source, GuardConst.getOnePlusAlertTitle());
                    if (judgeTextContains(info, appName) || judgeTextContains(info, shorfilmName)) {
                        AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "本应用执行安装卸载状态中，判断是否处于卸载");
                        info = findViewByID(source, GuardConst.getAlertMsg());
                        if (judgeTextContains(info, getString(R.string.powersaver_uninstall_text))) {
                            AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "本应用正准备执行卸载任务，自动单击取消");
                            performViewClick(findViewByID(source, GuardConst.getAlertCancelBtn()));
                            return;
                        }
                    }
                }
                if (GPResId.SETTINGS_PKG.equals(pkg)) {
                    if (event.getText() != null && !event.getText().isEmpty()) {
                        String title = event.getText().get(0).toString();
                        if (title.equals(getString(R.string.powersaver_title_accessibility_text))) {
                            AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "当前处于设置界面的无障碍界面处");
                            inPageState = PAGESTATE.ACCESSIBILITY;
                            return;
                        } else if (title.equals(appName)) {
                            info = traverseToFindFirstInfoContainsName(source, "android.widget.Switch", "");
                            if (info != null) {
                                AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "当前可能直接处于无障碍界面的开关处，后退之");
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                            }
                            return;
                        }
                    }

//                    info = findViewByID(source, GuardConst.getAlertMsg());
//                    if (judgeTextContains(info, getString(R.string.powersaver_uninstall_text))
//                            || judgeTextContains(info, getString(R.string.powersaver_force_stop_text))) {
//                        // 准备要卸载应用或者强制应用暂停
//                        AppDebugLog.d(AppDebugLog.TAG_SELF_GUARD, "本应用正准备执行卸载或者强制暂停操作，自动单击取消");
//                        performViewClick(findViewByID(source, GuardConst.getAlertCancelBtn()));
//                        return;
//                    }
                }
                break;
        }
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * 查找对应ID的View，返回首个非空节点
     */
    public AccessibilityNodeInfo findViewByID(AccessibilityNodeInfo source, String id) {
        List<AccessibilityNodeInfo> nodeInfoList = source.findAccessibilityNodeInfosByViewId(id);
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                if (nodeInfo != null) {
                    return nodeInfo;
                }
            }
        }
        return null;
    }

    /**
     * 模拟点击事件，会递归判断点击父节点
     */
    public void performViewClick(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        while (nodeInfo != null) {
            if (nodeInfo.isClickable()) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
            nodeInfo = nodeInfo.getParent();
        }
    }

    /**
     * 判断节点是否包含特定文本
     */
    protected boolean judgeTextContains(AccessibilityNodeInfo info, String msg) {
        if (info != null && info.getText() != null
                && info.getText().toString().contains(msg))
            return true;
        return false;
    }

    /**
     * 遍历查找首个含特定信息的节点
     */
    protected AccessibilityNodeInfo traverseToFindFirstInfoContainsName(AccessibilityNodeInfo info, String widgetName,
                                                                        String text) {
        if (info == null)
            return null;
        if ((TextUtils.isEmpty(widgetName) || info.getClassName().toString().contains(widgetName))
                && (TextUtils.isEmpty(text) || (info.getText() != null && info.getText().toString().contains(text)))) {
            return info;
        } else if (info.getChildCount() > 0) {
            AccessibilityNodeInfo result;
            for (int i = 0; i < info.getChildCount(); i++) {
                result = traverseToFindFirstInfoContainsName(info.getChild(i), widgetName, text);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

}
