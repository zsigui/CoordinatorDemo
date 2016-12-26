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
    int startIndex = -1, endIndex = -1;

    public void setAdapter(ListAdapter adapter) {
        mAdapter = adapter;
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
        int offsetTop = distanceY;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            v.layout(0, offsetTop, v.getMeasuredWidth(), offsetTop + v.getMeasuredHeight());
            offsetTop += v.getMeasuredHeight();
        }
    }

    int distanceY = 0;

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
                if (distanceY > 0) {
                    distanceY += (moveY > 0 ? moveY * 0.5f : moveY);
                } else if (allowMaxDistance > 0 && distanceY < -allowMaxDistance) {
                    distanceY += moveY;
                } else {
                    distanceY += moveY;
                }
                requestLayout();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (distanceY > 0) {
                    // 起始回弹，可通过 Scroller 来减速回弹
                    int diffY = -distanceY;
                    lastScrollY = 0;
                    mScroller.startScroll(0, 0, 0,  diffY, computeScrollDuration(-diffY));
                } else if (distanceY < -allowMaxDistance) {
                    // 因为 distanceY < 0, allowMaxDistance > 0, -allowMaxDistance < distanceY, 故 diffY > 0
                    int diffY = -allowMaxDistance - distanceY;
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
            distanceY += (y - lastScrollY);
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
