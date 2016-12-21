package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import com.jackiez.materialdemo.R;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2016/12/21
 */

public class TestView extends ViewGroup {
    public TestView(Context context) {
        super(context);
    }

    public TestView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(getContext());
    }

    Scroller mScroller;

    private boolean isInit = false;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (isInit || getChildCount() == 0) {
            View child;
            for (int i = 0 ; i < 3; i++) {
                child = LayoutInflater.from(getContext()).inflate(R.layout.list_item_text, this, false);
                measureChild(child);
                addViewInLayout(child, i, child.getLayoutParams(), true);
            }
        }
//        if (!changed)
//            return;
        int offsetTop = distanceY;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            v.layout(0, offsetTop, v.getMeasuredWidth(), offsetTop + v.getMeasuredHeight());
            offsetTop += v.getMeasuredHeight();
            Log.d("test", "offsetTop = " + offsetTop);
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

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d("test", "motionevent = " + ev.getActionMasked());
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
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                int moveY = (int) (event.getY() - lastY);
                lastY = (int) event.getY();
                int diffY = (int) (event.getY() - downY);
                Log.d("test", "onTouchEvent.moveY = " + moveY + ", diffY = " + diffY + ", getTop = " + getTop()
                 + ", child.getBottom = " + getChildAt(0).getBottom() + ", child.getTop = " + getChildAt(0).getTop()
                 + ", child1.getTop = " + getChildAt(1).getTop());
//                scrollBy(0, moveY);
                distanceY += moveY;
                requestLayout();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Log.d("test", "scroll to 0");
                scrollTo(0, 0);
                distanceY = 0;
                requestLayout();
                break;
        }
        super.onTouchEvent(event);
        return true;
    }
}
