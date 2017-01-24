package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.Scroller;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2016/12/21
 */

public class TestView extends ViewGroup {


    private int startDistanceY;

    public TestView(Context context) {
        this(context, null);
    }

    public TestView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(getContext());
    }

    Scroller mScroller;
    ListAdapter mAdapter;

    public void setAdapter(ListAdapter adapter) {
        mAdapter = adapter;
        requestLayout();
    }
    private int startIndex = -1;
    private int endIndex = 0;
    private int currentViewTotalHeight = 0;
    private int startViewY = 0;
    private int endViewY = 0;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mAdapter == null || mAdapter.getCount() == 0) {
            clearViewAndState();
            return;
        }
        final int viewHeight = getHeight();
        // 初始化
        if (currentViewTotalHeight == 0) {
            removeAllViewsInLayout();
            endViewY = startViewY = 0;
            startIndex = -1;
            endIndex = 0;
            View child;
            while (endViewY < viewHeight && endIndex < mAdapter.getCount()) {
                child = mAdapter.getView(endIndex ++, null, this);
                checkViewNotNull(child);
                measureChild(child);
                endViewY += child.getMeasuredHeight();
                currentViewTotalHeight += child.getMeasuredHeight();
            }
        }

        // 填充上下空缺子视图
        if (getChildCount() > 0 && currentViewTotalHeight != 0) {
            View child;
            while (startViewY > 0 && startIndex < endIndex
                    && startIndex > -1) {
                child = mAdapter.getView(startIndex --, null, this);
                checkViewNotNull(child);
                measureChild(child);
                addViewInLayout(child, 0, child.getLayoutParams(), true);
                currentViewTotalHeight += child.getMeasuredHeight();
                startViewY -= child.getMeasuredHeight();
            }

            while (endViewY < viewHeight && startIndex < endViewY
                    && endViewY < mAdapter.getCount()) {
                child = mAdapter.getView(endIndex ++, null, this);
                checkViewNotNull(child);
                measureChild(child);
                addViewInLayout(child, getChildCount() - 1, child.getLayoutParams(), true);
                currentViewTotalHeight += child.getMeasuredHeight();
                endViewY += child.getMeasuredHeight();
            }


            // 移除上下多余子视图
            child = getChildAt(0);
            while (child != null && startViewY + child.getMeasuredHeight() < 0
                    && startIndex < endIndex) {
                removeViewInLayout(child);
                startViewY += child.getMeasuredHeight();
                currentViewTotalHeight -= child.getMeasuredHeight();
                startIndex ++;
                child = getChildAt(0);
            }

            child = getChildAt(getChildCount() - 1);
            while (child != null && endViewY - child.getMeasuredHeight() > viewHeight
                    && startIndex < endIndex) {
                removeViewInLayout(child);
                endViewY -= child.getMeasuredHeight();
                currentViewTotalHeight -= child.getMeasuredHeight();
                endIndex --;
                child = getChildAt(getChildCount() - 1);
            }

            // 进行子视图布局
            int offsetTop = startViewY;
            for (int i = 0; i < getChildCount(); i++) {
                child = getChildAt(i);
                child.layout(0, offsetTop, child.getMeasuredWidth(), offsetTop + child.getMeasuredHeight());
                offsetTop += child.getMeasuredHeight();
            }
        }
    }

    private void checkViewNotNull(View child) {
        if (child == null) {
            throw new IllegalArgumentException("Adapter.getView() need to return not null view!");
        }
    }

    private void clearViewAndState() {
        startIndex = -1;
        endIndex = 0;
        startViewY = endViewY = 0;
        currentViewTotalHeight = 0;
        removeAllViewsInLayout();
    }

    private void measureChild(View child) {
        if (child == null)
            return;
        LayoutParams lp = child.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        child.setLayoutParams(lp);
        child.measure(getMeasuredWidthAndState(), MeasureSpec.UNSPECIFIED);
    }

    boolean isDrag;
    int downX, downY, lastX, lastY;
    int lastScrollY = 0;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d("testAddApkDownloadInfo", "motion event = " + ev.getActionMasked());
        if (isDrag && ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            return true;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) ev.getX();
                downY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                int diffX = (int) Math.abs(ev.getX() - downX);
                int diffY = (int) Math.abs(ev.getY() - downY);
                Log.d("testAddApkDownloadInfo", "diffY = " + diffY + ", diffX = " + diffX);
                if (diffY > diffX) {
                    isDrag = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isDrag = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mScroller.abortAnimation();
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                mScroller.abortAnimation();
                int moveY = (int) (event.getY() - lastY);
                lastY = (int) event.getY();
                startViewY += moveY;
                endViewY += moveY;
                requestLayout();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                break;
        }
//        return super.onTouchEvent(event) || isDrag;
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            final int y = mScroller.getCurrY();
            startDistanceY += (y - lastScrollY);
            lastScrollY = y;
            requestLayout();
        }
    }

    private int computeScrollDuration(int dy) {
        final int duration = 300; // MIN
        final int acceleration = 50;
        // at most 1s，at least 100ms
        return Math.min(duration + (int) Math.sqrt(dy / acceleration), 1500);
    }
}
