package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

/**
 * Scroller滚动
 *
 * @author csdn chunqiuwei
 */
public class HListView extends ViewGroup {
    /**
     * 存储数据用的Adapter
     **/
    private ListAdapter listAdapter;

    private int rightIndex = 0;
    private int leftIndex = -1;

    private GestureDetector gestureDetector;
    private int leftOffset = 0;
    private int scrollXMax = Integer.MAX_VALUE;
    /**
     * 滚动的总位移
     **/
    private int totalDistanceX = 0;
    /**
     * 上次调用onScroll之前滚动的总位移
     */
    private int preTotalDistanceX = 0;

    public HListView(Context context) {
        super(context);
        initParams();
    }

    public HListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initParams();
    }

    public HListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initParams();
    }

    private void initParams() {
        gestureDetector = new GestureDetector(getContext(), grestureListener);
    }

    private OnGestureListener grestureListener = new GestureDetector.SimpleOnGestureListener() {

        /**
         * 处理手指 由1个MotionEvent ACTION_DOWN, 多个ACTION_MOVE触发
         *
         * @param e1
         *            手势起点的移动事件
         * @param e2
         *            当前手势点的移动事件
         * @param distanceX distance是上一次的event2 减去 当前event2得到的结果
        lastEvent2 - event2 = distance
         *            距离上次调用onScroll方法的时候x轴滚动的距离
         * @param distanceY
         *            距离上次调用onScroll 方法的时候y轴滚动的距离
         */
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            totalDistanceX += distanceX;
            requestLayout();
            return true;

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    };

    // @Override
    public ListAdapter getAdapter() {
        return listAdapter;
    }

    // @Override
    public void setAdapter(ListAdapter adapter) {
        this.listAdapter = adapter;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return gestureDetector.onTouchEvent(event) || true;
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
            params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);
        }

        view.measure(
                MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));
        return view;

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {

        if (listAdapter == null) {
            return;
        }
        /**确保党左边是第一个的时候不再滚动*/
        if (totalDistanceX <= 0) {
            totalDistanceX = 0;
        }

        if (totalDistanceX > this.scrollXMax) {
            totalDistanceX = scrollXMax;
        }

        int distanceX = totalDistanceX - preTotalDistanceX;
        removeAnvisiableViews(-distanceX);
        addRightChildViews(-distanceX);
        addLeftChildViews(-distanceX);
        layoutChildViews(-distanceX);
        preTotalDistanceX = totalDistanceX;

    }

    /***
     * 删除看不见的view
     */
    private void removeAnvisiableViews(int distanceX) {
        // 移除左边看不到的view
        View firtVisiableView = getChildAt(0);
        if (firtVisiableView != null
                && distanceX + firtVisiableView.getRight() <= 0) {
            removeViewInLayout(firtVisiableView);
            leftOffset += firtVisiableView.getMeasuredWidth();
            leftIndex++;
        }

        // 移除右边看不到的view
        View lastVisialbeView = getChildAt(getChildCount() - 1);
        if (lastVisialbeView != null
                && lastVisialbeView.getLeft() + distanceX >= getWidth()) {
            removeViewInLayout(lastVisialbeView);
            rightIndex--;
        }

    }

    private boolean isLast = false;

    private void addRightChildViews(int distanceX) {
        // 2.让屏幕尽可能的显示Item。注意刚开始的时候是没有
        View rightChildView = getChildAt(getChildCount() - 1);

        // 获取此childView右边框距离parentView左边框的距离
        int rightEdge = rightChildView != null ? rightChildView.getRight() : 0;
        while (rightEdge + distanceX < getWidth()
                && rightIndex < listAdapter.getCount()) {
            View child = listAdapter.getView(rightIndex, null, null);
            child = measureChild(child);
            addViewInLayout(child, -1, child.getLayoutParams(), true);
            rightEdge += child.getMeasuredWidth();

            if (rightIndex == listAdapter.getCount() - 1) {
                scrollXMax = rightEdge + preTotalDistanceX - getWidth();
            }


            rightIndex++;
        }
    }

    private void addLeftChildViews(int distanceX) {
        View leftChildView = getChildAt(0);
        int leftEdge = leftChildView != null ? leftChildView.getLeft() : 0;
        while (leftEdge + distanceX > 0 && leftIndex >= 0) {
            View child = listAdapter.getView(leftIndex, null, null);
            child = measureChild(child);
            addViewInLayout(child, 0, child.getLayoutParams(), true);
            leftEdge -= child.getMeasuredWidth();
            leftIndex--;
            leftOffset -= child.getMeasuredWidth();
        }
    }

    /**
     * 3.把步骤2添加的view通过Layout布局到parentView中
     */
    private void layoutChildViews(int distanceX) {
        if (getChildCount() == 0) {
            return;
        }
        leftOffset += distanceX;
        int childLeft = leftOffset;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int childWidth = child.getMeasuredWidth();
            child.layout(childLeft, 0, childWidth + childLeft,
                    child.getMeasuredHeight());
            // 不过最好的写法是
            childLeft += childWidth + child.getPaddingRight();
        }

    }
}