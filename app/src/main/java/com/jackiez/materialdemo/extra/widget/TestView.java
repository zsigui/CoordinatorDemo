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

    private boolean isInit = false;
    // 最顶至可见位置的高度和
    private int allowMaxDistance = 0;
    // 暂时使用 listAdapter，后面修改
    private ListAdapter mAdapter;
    int startIndex = -1, endIndex = 0;
    // 表示第一项被遮盖部分的距离(被遮盖的情况下，否则 offset = 0，正数表达)
    private int offsetTop = 0;
    // 表示最后一项被遮盖部分的距离(被遮盖的情况下，否则 offset = 0，正数表达)
    private int offsetBottom = 0;
    private int totalHeight = 0;
    private int visibleBottomY = 0;
    private int currentTotalHeight = 0;

    public void setAdapter(ListAdapter adapter) {
        mAdapter = adapter;
        requestLayout();
    }

    public void notifyChange() {
        currentTotalHeight = 0;
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        if (isInit || getChildCount() == 0) {
//            View child;
//            for (int i = 0 ; i < 30; i++) {
//                child = LayoutInflater.from(getContext()).inflate(R.layout.list_item_text, this, false);
//                ((TextView) child.findViewById(R.id.tv_content)).setText("测试位置" + i);
//                measureChild(child);
//                addViewInLayout(child, i, child.getLayoutParams(), true);
//            }
//        }
//        if (!changed)
//            return;

        if (mAdapter == null || !changed)
            return;

        if (currentTotalHeight == 0) {
            // 初始添加视图
            if (getChildCount() != 0)
                removeAllViewsInLayout();
            int i = 0;
            View child;
            visibleBottomY = startDistanceY;
            while (i < mAdapter.getCount()
                    && visibleBottomY < getHeight()) {
                child = mAdapter.getView(endIndex, null, this);
                measureChild(child);
                visibleBottomY += child.getMeasuredHeight();
                currentTotalHeight += child.getMeasuredHeight();
            }
        }


        // 当前移动位置
        int moveY = 0;
        if (moveY > 0) {
            // 表示下滑，顶部添加视图
            moveY -= offsetTop;
            View first;
            while (startIndex < endIndex
                    && startIndex > -1
                    && moveY > 0) {
                first = mAdapter.getView(startIndex, null, this);
                measureChild(first);
                moveY -= first.getMeasuredHeight();
            }
        } else {
            // 表示上滑，底部添加视图
            moveY = -moveY;
            moveY -= offsetTop;
            View last;
            while (startIndex < endIndex
                    && endIndex < mAdapter.getCount()
                    && moveY > 0) {
                last = mAdapter.getView(endIndex, null, this);
                measureChild(last);
                moveY -= last.getMeasuredHeight();
            }
        }



        // 移除看不见的视图
        if (getChildCount() > 0) {
            View first = getChildAt(0);
            while (first != null && first.getBottom() < getTop()
                    && startIndex < endIndex) {
                removeViewInLayout(first);
                startIndex ++;
                first = getChildAt(0);
            }
            View last = getChildAt(getChildCount() - 1);
            while (last != null && last.getTop() > getBottom()
                    && startIndex < endIndex) {
                removeViewInLayout(last);
                endIndex --;
                last = getChildAt(getChildCount() - 1);
            }
        }

        // 添加顶端视图


        // 添加底部视图

        // 进行视图布局
        int offsetTop = startDistanceY;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            v.layout(0, offsetTop, v.getMeasuredWidth(), offsetTop + v.getMeasuredHeight());
            offsetTop += v.getMeasuredHeight();
        }
    }

    int startDistanceY = 0;

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
        Log.d("test", "motion event = " + ev.getActionMasked());
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
                Log.d("test", "diffY = " + diffY + ", diffX = " + diffX);
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
//                int diffY = (int) (event.getY() - downY);
                if (startDistanceY > 0) {
                    startDistanceY += (moveY > 0 ? moveY * 0.5f : moveY);
                } else if (allowMaxDistance > 0 && startDistanceY < -allowMaxDistance) {
                    startDistanceY += moveY;
                } else {
                    startDistanceY += moveY;
                }
                requestLayout();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (startDistanceY > 0) {
                    // 起始回弹，可通过 Scroller 来减速回弹
                    int diffY = -startDistanceY;
                    lastScrollY = 0;
                    mScroller.startScroll(0, 0, 0,  diffY, computeScrollDuration(-diffY));
                } else if (startDistanceY < -allowMaxDistance) {
                    // 因为 startDistanceY < 0, allowMaxDistance > 0, -allowMaxDistance < startDistanceY, 故 diffY > 0
                    int diffY = -allowMaxDistance - startDistanceY;
                    lastScrollY = 0;
                    mScroller.startScroll(0, 0, 0, diffY, computeScrollDuration(diffY));
                }
                requestLayout();
                invalidate();
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
