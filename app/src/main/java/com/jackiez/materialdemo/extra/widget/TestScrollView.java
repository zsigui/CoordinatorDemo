package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jackiez.materialdemo.R;

import java.util.ArrayList;

/**
 * Created by zsigui on 16-12-20.
 */

public class TestScrollView extends ViewGroup {

    // 指示当前
    private int totalDistanceY = 0;
    // 指示总的高度
    private int totalHeight = Integer.MAX_VALUE;
    private int viewHeight;
    private int topOffset = 0;

    private int firstPos = -1;
    private int lastPos = 0;

    private BaseAdapter mAdapter;

    public TestScrollView(Context context) {
        super(context);
    }

    public TestScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (itemCount == 0) {
            return;
        }
        // 保证滑动到顶端不再滑动
        if (totalDistanceY <= 0) {
            totalDistanceY = 0;
        }
        if (totalDistanceY >= totalHeight) {
            totalDistanceY = totalHeight;
        }

        int moveDistanceY = 0;

        int realDistanceY = 0;
        if (moveDistanceY > topOffset + totalDistanceY) {
            // 移动到开始
            realDistanceY = topOffset + totalDistanceY;
        } else if (moveDistanceY > topOffset) {
            realDistanceY = moveDistanceY;
        } else {
            realDistanceY = 0;
        }
        removeUnVisibleView();
        addChildViewAbove(realDistanceY);
        addChildViewBelow();
        layoutChildView();
    }

    private void layoutChildView() {
        if (getChildCount() == 0) {
            return;
        }

        int startTop = 0;
        View child;
        for (int i = 0; i < getChildCount(); i++) {
            child = getChildAt(i);
            child.layout(0, startTop, child.getMeasuredWidth(), startTop + child.getMeasuredHeight());
            startTop += child.getMeasuredHeight();
        }
    }

    private void addChildViewBelow() {
        if (lastPos >= getChildCount()) {
            return;
        }
        View child = getChildAt(getChildCount() - 1);
    }

    private void addChildViewAbove(int distanceY) {
        if (itemCount == 0) {
            return;
        }
        while (firstPos >= 0
                && distanceY > 0 ) {
            View child = mAdapter.getView(firstPos, null, this);
            child = measureChild(child);
            addViewInLayout(child, 0, child.getLayoutParams(), true);
            topOffset -= child.getMeasuredHeight();
            firstPos --;
        }

    }

    /**
     * 测量每个child的宽和高
     *
     * @param view
     * @return
     */
    private View measureChild(View view) {
        LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);
        }

        view.measure(getMeasuredWidthAndState(), MeasureSpec.UNSPECIFIED);
        return view;

    }

    private void removeUnVisibleView() {
        View first = getChildAt(0);
        Log.d("test", "removeUnVisibleView.getTop = " + getTop() + ", first is null ? " + first +
                ", first.getBottom() = " + (first == null ? 0 : first.getBottom()));
        while (first != null
                && first.getBottom() < getTop()) {
            removeViewInLayout(first);
            topOffset += first.getMeasuredHeight();
            first = getChildAt(0);
            firstPos ++;
        }

        View last = getChildAt(getChildCount() - 1);
        while (last != null
                && last.getTop() < getBottom()) {
            removeViewInLayout(last);
            last = getChildAt(getChildCount() - 1);
            lastPos --;
        }
    }


    boolean mIsInit = false;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        if (getMeasuredHeight() != 0 && !mIsInit) {
//            initAddView();
//            mIsInit = true;
//        }
        viewHeight = getMeasuredHeight();
    }


    float downX;
    float downY;
    float lastX;
    float lastY;
    final int SLOP = 15;
    private boolean mIsDrag = true;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mIsDrag && ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            return true;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float diffY = Math.abs(ev.getY() - downY);
                float diffX = Math.abs(ev.getX() - downX);
                if (diffY > SLOP && diffY > diffX) {
                    mIsDrag = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDrag = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                Log.d("message", "touch.down.y = " + getScrollY());
                break;
            case MotionEvent.ACTION_MOVE:
                int diffY = (int) (event.getY() - lastY);
                lastX = event.getX();
                lastY = event.getY();
                if (itemCount > 0) {
                    // 当itemCount为空表示无项可滑动
                    int actualScrollDiff = 0;
                    if (diffY > 0) {
                        // 下滑
                        if (firstVisibleIndex == 0 && getChildAt(0).getTop() >= getTop()) {
                            // 已到顶部，不可滑动
                            return true;
                        }
                        int h = 0;
                        while (diffY > 0) {
                            final int dy = (h == 0 ? getTop() - getChildAt(0).getTop() : h);
                            if (dy < diffY) {
                                actualScrollDiff += dy;
                                final View v = attachOrAddViewAboveIfNeed();
                                if (v != null) {
                                    h = v.getMeasuredHeight();
                                    firstVisibleIndex -= 1;
                                    lastVisibleIndex -= 1;
                                } else {
                                    // 到顶部了
                                    break;
                                }
                            }
                            diffY -= dy;
                        }

                    } else {
                        if (lastVisibleIndex == itemCount - 1 && getChildAt(getChildCount() - 1).getBottom() <= getBottom()) {
                            return true;
                        }
                        int h = 0;
                        while (diffY < 0) {
                            final int dy = (h == 0 ? getBottom() - getChildAt(getChildCount() - 1).getBottom() : -h);
                            if (dy > diffY) {
                                actualScrollDiff += dy;
                                final View v = attachOrAddViewBelowIfNeed();
                                if (v != null) {
                                    h = v.getMeasuredHeight();
                                    firstVisibleIndex += 1;
                                    lastVisibleIndex += 1;
                                } else {
                                    // 到底部了
                                    break;
                                }
                            }
                            diffY -= dy;
                        }
                        // 上滑
                    }
                    scrollBy(0, actualScrollDiff);
                    trimItemToAdaptIndex(actualScrollDiff < 0);
                }
                Log.d("message", "touch.move.y = " + getScrollY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Log.d("message", "touch.up.y = " + getScrollY());
                break;
        }
        super.onTouchEvent(event);
        return true;
    }

    private void trimItemToAdaptIndex(boolean isRollUp) {
        if (itemCount < 0) {
            return;
        }
        if (isRollUp) {
            // 对于下滑
            View last = getChildAt(getChildCount() - 1);
            while (last.getTop() < getBottom()) {
                detachOrRemoveFootViewIfNeed();
                if (getChildCount() > 0) {
                    last = getChildAt(getChildCount() - 1);
                }
            }
        } else {
            // 对于上滑
            View first = getChildAt(0);
            while (first.getBottom() < getTop()) {
                detachOrRemoveHeadViewIfNeed();
            }
        }
    }

    private ArrayList<View> cacheDetachedView = new ArrayList<>();
    private SparseArray<View> activeView = new SparseArray<>();
    private int itemCount = 20;
    private int firstVisibleIndex = -1;
    private int lastVisibleIndex = -1;

    public void initAddView() {
        if (activeView.size() != 0)
            return;
        firstVisibleIndex = -1;
        View v = attachOrAddViewBelowIfNeed();
        firstVisibleIndex = lastVisibleIndex = 0;
        int totalHeight = v.getHeight();
        Log.d("test", "totalHeight = " + totalHeight + ", measureHeight= " + getMeasuredHeight() + ", bottom = " + getBottom() + ", v.bottom = " + v.getBottom());
        while (totalHeight < getMeasuredHeight()) {
            v = attachOrAddViewBelowIfNeed();
            if (v == null) {
                // 已有项不够填充满界面
                break;
            }
            Log.d("test", "填充项 + " + lastVisibleIndex + ", totalHeight = " + totalHeight + ", height = " + v.getHeight());
            totalHeight += v.getHeight();
            lastVisibleIndex ++;
        }
    }

    public View attachOrAddViewBelowIfNeed() {
        if (firstVisibleIndex + getChildCount() >= itemCount) {
            return null;
        }
        int i = getChildCount();
        View v;
        LayoutParams lp;
        if (cacheDetachedView.size() == 0) {
            v = LayoutInflater.from(getContext()).inflate(R.layout.list_item_text, this, false);
//            v.setLayoutParams(generateDefaultLayoutParams());
            v.measure(getMeasuredWidthAndState(), MeasureSpec.UNSPECIFIED);
            addView(v);
        } else {
            v = cacheDetachedView.remove(0);
            lp = (LayoutParams) v.getLayoutParams();
            if (lp == null) {
                lp = generateDefaultLayoutParams();
            }
            attachViewToParent(v, i, lp);
        }
        activeView.put(firstVisibleIndex + i + 1, v);
        return v;
    }

    public View attachOrAddViewAboveIfNeed() {
        if (firstVisibleIndex < 0) {
            return null;
        }
        View v;
        LayoutParams lp;
        int height = 0;
        if (cacheDetachedView.size() == 0) {
            v = LayoutInflater.from(getContext()).inflate(R.layout.list_item_text, this, false);
            v.setLayoutParams(generateDefaultLayoutParams());
            v.measure(getMeasuredWidthAndState(), MeasureSpec.UNSPECIFIED);
            height = v.getMeasuredHeight();
            Log.d("test-a", "add view height = " + v.getHeight() + ", mH = " + v.getMeasuredHeight());
            addView(v, 0);
        } else {
            v = cacheDetachedView.remove(0);
            lp = (LayoutParams) v.getLayoutParams();
            if (lp == null) {
                lp = generateDefaultLayoutParams();
            }
            height = v.getHeight();
            attachViewToParent(v, 0, lp);
            Log.d("test-a", "attach view height = " + v.getHeight() + ", mH = " + v.getMeasuredHeight());
        }
        activeView.put(firstVisibleIndex + 1, v);
        // 进行偏移保证此前的位置
        scrollBy(0, -v.getMeasuredHeight());
        onBindView(v, firstVisibleIndex + 1);
        return v;
    }

    private void onBindView(View v, int i) {
        final TextView tv = (TextView) v.findViewById(R.id.tv_content);
        tv.setText("当前View的下标位置为" + i);
    }

    public View detachOrRemoveHeadViewIfNeed() {
        if (getChildCount() == 0) {
            return null;
        }
        View v = getChildAt(0);
        activeView.removeAt(0);
        detachViewFromParent(v);
        if (cacheDetachedView.size() < 10)
            cacheDetachedView.add(v);
        else
        removeDetachedView(v, false);
        scrollBy(0, v.getMeasuredHeight());
        return v;
    }

    public View detachOrRemoveFootViewIfNeed() {
        if (getChildCount() == 0) {
            return null;
        }
        View v = getChildAt(getChildCount() - 1);
        activeView.removeAt(activeView.size() - 1);
        detachViewFromParent(v);
        if (cacheDetachedView.size() < 10)
            cacheDetachedView.add(v);
        else
            removeDetachedView(v, false);
        return v;
    }
}
