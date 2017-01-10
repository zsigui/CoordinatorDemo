package com.jackiez.materialdemo.extra.widget;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2017/1/10
 */

public class SwipeBackView extends FrameLayout {
    private static final int MIN_SLOP = 15;

    public SwipeBackView(Context context) {
        this(context, null);
    }

    public SwipeBackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeBackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.BLACK);
        mScroller = new Scroller(context);
    }

    private View child;
    public SwipeBackListener mListener;
    private SwipeBackListener mNestListener = new SwipeBackListener() {
        @Override
        public void onSwipe(float offset) {
            getBackground().setAlpha((int) ((1 - offset) * 255));
            if (mListener != null) {
                mListener.onSwipe(offset);
            }
        }

        @Override
        public void onFinishSwipe() {
            if (mListener != null) {
                mListener.onFinishSwipe();
            }
        }
    };
    private boolean mIsDrag;
    private Scroller mScroller;
    private VelocityTracker mTracker;
    private float mCurTranslateX;
    private float mTotalTranslateX;
    private ObjectAnimator mAnimator;

    private float mDownX, mDownY, mLastX;

    public void attachView(View v, SwipeBackListener listener) {
        child = v;
        mListener = listener;
    }

    public void setListener(SwipeBackListener listener) {
        mListener = listener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() == 1) {
            child = getChildAt(0);
        } else if (getChildCount() > 1) {
            throw new IllegalStateException("SwipeBackView can only hold one view!");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mTotalTranslateX = getMeasuredWidth();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mScroller.abortAnimation();
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        if (child == null) {
            return super.onInterceptTouchEvent(ev);
        }
        if (mIsDrag) {
            return true;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = ev.getX();
                mDownY = ev.getY();
                mLastX = ev.getX();
                clearOrCreateTrackerIfNotExist();
                mTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                createTrackerIfNotExist();
                mTracker.addMovement(ev);
                mTracker.computeCurrentVelocity(100, 100);
                int diffX = (int) (ev.getX() - mDownX);
                int diffY = (int) (ev.getY() - mDownY);
                if (diffX > MIN_SLOP && Math.abs(diffX) > Math.abs(diffY)) {
                    mIsDrag = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return mIsDrag;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDrag = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mScroller.abortAnimation();
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        if (child == null) {
            return super.onTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clearOrCreateTrackerIfNotExist();
                mTracker.addMovement(ev);
                mDownX = ev.getX();
                mDownY = ev.getY();
                mLastX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                createTrackerIfNotExist();
                mTracker.addMovement(ev);
                float diffX = ev.getX() - mLastX;
                mLastX = ev.getX();
                if (mCurTranslateX + diffX < 0) {
//                    child.setTranslationX(0);
                    mCurTranslateX = 0;
                } else if (mCurTranslateX + diffX > mTotalTranslateX) {
//                    child.setTranslationX(mTotalTranslateX);
                    mCurTranslateX = mTotalTranslateX;
                } else {
                    mCurTranslateX += diffX;
                }
                child.setTranslationX(mCurTranslateX);
//                if (mListener != null && mTotalTranslateX > 0) {
//                    mListener.onSwipe(mCurTranslateX / mTotalTranslateX);
//                }
                if (mTotalTranslateX > 0) {
                    mNestListener.onSwipe(mCurTranslateX / mTotalTranslateX);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDrag = false;
                createTrackerIfNotExist();
                mTracker.addMovement(ev);
                mTracker.computeCurrentVelocity(100, 1000);
                mScroller.fling((int) mCurTranslateX, 0, (int) mTracker.getXVelocity(), 0, 10, 1000, 0, 0);
                invalidate();
                clearTrackerIfExist();
//                handleSwipeAnimator();
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mCurTranslateX = mScroller.getCurrX();
            child.setTranslationX(mCurTranslateX);
            if (mTotalTranslateX > 0) {
                mNestListener.onSwipe(mCurTranslateX / mTotalTranslateX);
            }
            invalidate();
        } else {
            handleSwipeAnimator();
        }
    }

    private void handleSwipeAnimator() {
        if (mCurTranslateX / mTotalTranslateX > 0.5f) {
            // 执行滑动动画画出屏幕
            mAnimator = ObjectAnimator.ofFloat(child, "translateX", mCurTranslateX, mTotalTranslateX);
        } else {
            // 回复原来的位置
            mAnimator = ObjectAnimator.ofFloat(child, "translateX", mCurTranslateX, 0);
        }
//                mAnimator.setDuration((long) Math.max((500 * (1 - 2 * Math.abs(mCurTranslateX - mTotalTranslateX / 2) /
//                        mTotalTranslateX)), 200));
        mAnimator.setDuration(250);
        mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float newVal = (float) animation.getAnimatedValue();
                mCurTranslateX = newVal;
                child.setTranslationX(newVal);
                if (mCurTranslateX == mTotalTranslateX) {
//                            if (mListener != null) {
//                                mListener.onFinishSwipe();
//                            }
                    mNestListener.onFinishSwipe();
                } else {
//                            if (mListener != null) {
//                                mListener.onSwipe(mCurTranslateX / mTotalTranslateX);
//                            }
                    mNestListener.onSwipe(mCurTranslateX / mTotalTranslateX);
                }
            }
        });
        mAnimator.start();
    }

    private void createTrackerIfNotExist() {
        if (mTracker == null) {
            mTracker = VelocityTracker.obtain();
        }
    }

    private void clearOrCreateTrackerIfNotExist() {
        if (mTracker == null) {
            mTracker = VelocityTracker.obtain();
        } else {
            mTracker.clear();
        }
    }

    private void clearTrackerIfExist() {
        if (mTracker != null) {
            mTracker.clear();
            mTracker.recycle();
            mTracker = null;
        }
    }

    public interface SwipeBackListener {
        void onSwipe(float offset);

        void onFinishSwipe();
    }
}
