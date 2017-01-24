package com.luna.powersaver.gp;

import android.content.Context;
import android.util.SparseArray;

import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.service.GuardService;

/**
 * 屏保对外调用的门面
 *
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2017/1/13
 */
public class PowerSaver {

    private static boolean isInit = false;

    private static PowerSaver sInstance = new PowerSaver();

    private SparseArray<StateChangeCallback> mCallbacks = new SparseArray<>();

    private PowerSaver() {
    }

    public static PowerSaver get() {
        if (!isInit) {
            throw new IllegalStateException("Need to call init() first!");
        }
        return sInstance;
    }

    public void addCallback(StateChangeCallback callback) {
        if (callback == null)
            return;
        mCallbacks.put(callback.hashCode(), callback);
    }

    public void removeCallback(StateChangeCallback callback) {
        if (callback == null)
            return;
        mCallbacks.remove(callback.hashCode());
    }

    /**
     * 初始化调用请先调用该方法，最好在 Application.onCreate() 中进行调用
     */
    public static void init(Context context) {
        if (isInit) return;
        if (context == null) {
            throw new IllegalArgumentException("Context is not allow null!");
        }
        StaticConst.sContext = context.getApplicationContext() != null ? context : context.getApplicationContext();
        GuardService.testAliveAndCreateIfNot(StaticConst.sContext);
        isInit = true;
    }

    /**
     * 通知所有监听者弹窗显示
     */
    private void notifyShowCallback() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.valueAt(i).onGuardShow();
        }
    }

    /**
     * 通知所有监听者弹窗即将关闭
     */
    private void notifyHideCallback() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.valueAt(i).onGuardHide();
        }
    }

    /**
     * 通知所有监听者现在触摸中，已滑动比例
     */
    void notifyTouchCallback(float offset) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.valueAt(i).onGuardTouch(offset);
        }
    }

    private long mLastRecordTime = 0;

    /**
     * 显示屏保，如果成功显示（新创建）则会进行回调
     */
    public void showGuardView(Context context) {
        showGuardView(context, false, false);
    }

    /**
     * 显示屏保，该方法会直接回调 onGuardShow()
     */
    public void showGuardView(Context context, boolean isScreenOn, boolean isFocusable) {
        boolean isNowShow = ViewManager.get().showGuardView(context, isScreenOn, isFocusable);
//        if (isNowShow) {
        mLastRecordTime = System.currentTimeMillis();
        notifyShowCallback();
//        }
    }

    /**
     * 隐藏屏保，该方法会直接回调 onGuardHide()
     */
    public void hideGuardView(Context context) {
//        if (ViewManager.get().isGuardViewShown()) {
        notifyHideCallback();
//        }
        ViewManager.get().hideGuardView(context);
    }

    /**
     * 获取该次锁屏至今持续的时间 (ms)
     */
    public long obtainShowTimeInMills() {
        return isGuardViewShown() ? 0 : (System.currentTimeMillis() - mLastRecordTime);
    }

    public boolean isGuardViewShown() {
        return ViewManager.get().isGuardViewShown();
    }

    /**
     * 当屏保开启或关闭时进行状态变化回调函数
     */
    public interface StateChangeCallback {

        void onGuardShow();

        void onGuardTouch(float offset);

        void onGuardHide();
    }
}
