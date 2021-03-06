package com.luna.powersaver.gp;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.manager.BatteryTimeManager;
import com.luna.powersaver.gp.utils.DateUtil;
import com.luna.powersaver.gp.view.CircleProgressView;
import com.luna.powersaver.gp.view.SwipeBackView;

import java.lang.ref.WeakReference;

/**
 * Created by zsigui on 17-1-9.
 */

class ViewManager {

    private static ViewManager sInstance = new ViewManager();

    static ViewManager get() {
        return sInstance;
    }

    private static final int UPDATE_INTERVAL = 1000;
    private SwipeBackView mContentView;
    private ViewHolder mViewHolder;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    class UpdateRunnable implements Runnable {

        private WeakReference<ViewManager> mReference;

        UpdateRunnable(ViewManager vm) {
            mReference = new WeakReference<>(vm);
        }

        @Override
        public void run() {
            if (mReference == null || mReference.get() == null) {
                return;
            }
            ViewManager vm = mReference.get();
            if (vm.mViewHolder != null) {
                vm.mViewHolder.bindData(StaticConst.sContext);
                vm.mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    }

    /**
     * 显示充电屏保
     *
     * @return 如果是首次创建并且创建完成，返回true，否则返回false
     */
    boolean showGuardView(Context context, boolean isScreenOn, boolean isFocusable) {
        if (mContentView == null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams lp = generateLayoutParam(isScreenOn, isFocusable);
            initView(context);
            wm.addView(mContentView, lp);
            return true;
        }
        return false;
    }

    /**
     * 隐藏充电屏保
     *
     * @return 如果成功关闭已存在屏保，返回true，否则返回false
     */
    boolean hideGuardView(Context context) {
        if (mContentView != null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mContentView);
            destroyView();
            return true;
        }
        return false;
    }

    boolean isGuardViewShown() {
        return mContentView != null;
    }

    private void destroyView() {
        mContentView = null;
        if (mViewHolder != null) {
            mViewHolder.contentView = null;
            mViewHolder = null;
        }
    }

    private void initView(Context context) {
        mViewHolder = new ViewHolder(context);
        mContentView = (SwipeBackView) mViewHolder.contentView;
        mViewHolder.bindData(context);
        mHandler.postDelayed(new UpdateRunnable(this), UPDATE_INTERVAL);
        mContentView.setListener(new SwipeBackView.SwipeBackListener() {
            @Override
            public void onSwipe(float offset) {
                PowerSaver.get().notifyTouchCallback(offset);
            }

            @Override
            public void onFinishSwipe() {
                PowerSaver.get().hideGuardView(StaticConst.sContext);
            }
        });
    }

    private WindowManager.LayoutParams generateLayoutParam(boolean isScreenOn, boolean isFocusable) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        lp.format = PixelFormat.RGBA_8888;
        lp.flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if (!isFocusable) {
            lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        }
        if (isScreenOn) {
            lp.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // 4.4 以下要覆盖需要申请弹窗权限，因为其下 toast 不支持触摸事件
            lp.type = WindowManager.LayoutParams.TYPE_PHONE;
        } else {
            lp.type = WindowManager.LayoutParams.TYPE_TOAST;
        }
        lp.gravity = Gravity.FILL | Gravity.LEFT | Gravity.TOP;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            lp.systemUiVisibility = View.INVISIBLE;
        } else {
            lp.systemUiVisibility = View.INVISIBLE;
        }
        return lp;
    }

    private static class ViewHolder {
        View contentView;
        TextView tvTime;
        TextView tvWeek;
        TextView tvDate;
        CheckedTextView ctvQuick;
        CheckedTextView ctvContinuous;
        CheckedTextView ctvTrickle;
        CheckedTextView[] ctv;
        CircleProgressView pb;
        TextView tvRemain;

        ViewHolder(Context context) {
            contentView = LayoutInflater.from(context).inflate(R.layout.powersaver_main_layout, null);
            tvTime = (TextView) contentView.findViewById(R.id.powersaver_tv_time);
            tvWeek = (TextView) contentView.findViewById(R.id.powersaver_tv_week);
            tvDate = (TextView) contentView.findViewById(R.id.powersaver_tv_date);
            ctvQuick = (CheckedTextView) contentView.findViewById(R.id.powersaver_ctv_quick);
            ctvContinuous = (CheckedTextView) contentView.findViewById(R.id.powersaver_ctv_continuous);
            ctvTrickle = (CheckedTextView) contentView.findViewById(R.id.powersaver_ctv_trickle);
            pb = (CircleProgressView) contentView.findViewById(R.id.powersaver_pb);
            tvRemain = (TextView) contentView.findViewById(R.id.powersaver_tv_charge_remain);
            ctv = new CheckedTextView[3];
            ctv[0] = ctvQuick;
            ctv[1] = ctvContinuous;
            ctv[2] = ctvTrickle;
            // 设置高斯模糊背景
            contentView.setBackgroundResource(android.R.color.transparent);
//            ViewUtil.blur(context, ViewUtil.getCenterCropWallPaper(context), contentView);
//            ViewUtil.blur(context, ViewUtil.drawableToBitmap(context.getResources().getDrawable(R.drawable
//                    .powersaver_default_bg)), contentView);
        }

        void bindData(Context context) {
            String[] time = DateUtil.getCurrentTime();
            tvTime.setText(time[0]);
            tvWeek.setText(time[1]);
            tvDate.setText(time[2]);
            pb.setPercent(BatteryTimeManager.get().getPercent());
            long remain = BatteryTimeManager.get().calculateChargeTime();
            if (remain == 0) {
                tvRemain.setText(context.getString(R.string.powersaver_st_charge_full));
            } else {
                String s = DateUtil.formatTime(remain);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    tvRemain.setText(Html.fromHtml(String.format(
                            context.getString(R.string.powersaver_st_charge_remain), s),
                            Html.FROM_HTML_MODE_LEGACY));
                } else {
                    tvRemain.setText(Html.fromHtml(String.format(
                            context.getString(R.string.powersaver_st_charge_remain), s)));
                }
            }
            int checkedIndex = BatteryTimeManager.get().getChargeState();
            for (int i = 0; i < ctv.length; i++) {
                ctv[i].setChecked(i == checkedIndex);
            }
        }
    }
}
