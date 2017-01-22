package com.luna.powersaver.gp;

import android.content.Context;
import android.util.SparseArray;

/**
 * 屏保对外调用的门面
 *
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2017/1/13
 */
public class PowerSaver {

    private static PowerSaver sInstance = new PowerSaver();

    private SparseArray<StateChangeCallback> mCallbacks = new SparseArray<>();

    private PowerSaver() {
    }

    public static PowerSaver get() {
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

        void onGuardHide();
    }
}
