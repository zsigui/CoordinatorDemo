package com.jackiez.materialdemo.extra.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.luna.powersaver.gp.PowerSaver;

import java.text.SimpleDateFormat;
import java.util.Date;
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
    // 该状态表明当前是否正处于完成任务状态
    public static boolean sIsInWork = false;
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
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        Log.w(TAG, "onAccessibilityEvent is called! type = " + event.getEventType()
                + ", action = " + event.getAction() + ", time = " + df.format(new Date(event.getEventTime()))
                + ", text = " + event.getText() + ", package = " + event.getPackageName()
                + ", source.text = " + (event.getSource() != null ? event.getSource().getText() : "null")
                + ", isWork = " + sIsInWork);
        traveselNodeInfo(getRootInActiveWindow(), 0);

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            finalWindowStateChangeEvent = event;
        }
        if (sIsInWork) {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (getRootInActiveWindow() != null) {
                    if (event.getPackageName() != null && "com.android.vending".equals(event.getPackageName())) {
                        Log.w(TAG, "Now in GP!");
                    }
                }
            }
        }
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


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Log.d(TAG, "接收到网络请求，进行下载事件，此处模拟包名: " + TEST_PACKAGE_NAME);
                    Log.d(TAG, "延迟" + JUDGE_WORK_INTERVAL + "秒执行GP下载操作");
//                    toast("接收到网络请求，进行下载事件，此处模拟包名：" + TEST_PACKAGE_NAME);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.sendEmptyMessage(1);
                        }
                    }, JUDGE_WORK_INTERVAL);
                    break;
                case 1:
                    Log.d(TAG, "判断当前是否还处于屏保并执行任务");
//                    toast("判断当前是否还处于屏保并执行任务");
                    if (PowerSaver.get().isGuardViewShown()) {
                        Log.d(TAG, "当前处于屏保中，可以执行GP任务了");
                        // 设置状态
                        sIsInWork = true;
                        // 跳转对应GP位置
                        jumpToStore(NBAccessibilityService.this, TEST_PACKAGE_NAME);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

                            }
                        }, 5000);
                    }
                    break;
            }
        }
    };

    public static final String TEST_PACKAGE_NAME = "com.luna.applocker.gp";

    public boolean jumpToStore(Context context, String packageName) {
        if (context == null)
            return false;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
