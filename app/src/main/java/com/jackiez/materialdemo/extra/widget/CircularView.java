package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Scroller;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by zsigui on 16-12-19.
 */

public class CircularView extends LinearLayout {

    static final int TYPE_NONE = -1;
    private static final int MIN_SLOP = 50;
    private static final int MIN_VELOCITY = 100;
    private float downX;
    private float downY;
    private float lastX;
    private float lastY;
    private int currentX;
    private int currentY;
    private int screenHeight;
    private boolean mIsDrag;

    private int mWidth;
    private int mHeight;

    private Scroller mScroller;
    private VelocityTracker mTracker;
    private BaseAdapter mAdapter;

    private boolean isReMeasure;

    private ArrayList<Integer> mShowingViewPosition = new ArrayList<>();
    private ArrayList<View> mShowingViews = new ArrayList<>();

    private LinkedList<View> mViews = new LinkedList<>();


    public CircularView(Context context) {
        this(context, null);
    }

    public CircularView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(getContext());
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
    }

    public void setAdapter(BaseAdapter adapter) {
        if (adapter == null) {
            Log.w("warn!", "you need to set a not null adapter");
            return;
        }
        mAdapter = adapter;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isReMeasure) {
            removeAllViews();
            mWidth = getMeasuredWidth();
            mHeight = getMeasuredHeight();
            if (mHeight == 0) {
                mHeight = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.UNSPECIFIED);
            }
            if (mAdapter != null) {
                BaseHolder holder;
                int type;
                int totalHeight = 0;
                for (int i = 0; i < mAdapter.getItemCount(); i++) {
                    type = mAdapter.getItemViewType(i);
                    holder = mAdapter.onCreateHolder(this, type);
                    if (holder != null) {
                        Log.d("test-view", i + " : height = " + holder.itemView.getMeasuredHeight());
                        holder.itemView.measure(widthMeasureSpec, mHeight);
                        Log.d("test-view", i + " : height = " + holder.itemView.getMeasuredHeight());
                        totalHeight += holder.itemView.getMeasuredHeight();
                        mShowingViews.add(holder.itemView);
                        mShowingViewPosition.add(i);
                    }
                    if (mHeight != 0 && totalHeight >= mHeight) {
                        if (mHeight == screenHeight) {

                        }
                        break;
                    }
                }
            }

        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mIsDrag && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = downX = event.getX();
                lastY = downY = event.getY();
                initOrResetVelocityTracker();
                mTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_MOVE:
                initVelocityTrackerIfNeeded();
                float diffX = event.getX() - lastX;
                float diffY = event.getY() - lastY;
                mTracker.addMovement(event);
                mTracker.computeCurrentVelocity(1000);
                if ((Math.abs(mTracker.getYVelocity()) > Math.abs(mTracker.getXVelocity())
                        && Math.abs(mTracker.getYVelocity()) > MIN_VELOCITY)
                        || (Math.abs(diffY) > MIN_SLOP && Math.abs(diffY) > Math.abs(diffX))) {
                    requestDisallowInterceptTouchEvent(true);
                    mIsDrag = true;
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                requestDisallowInterceptTouchEvent(false);
                mIsDrag = false;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initVelocityTrackerIfNeeded();
                mTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_MOVE:
                initVelocityTrackerIfNeeded();
                mTracker.addMovement(event);
                scrollBy(0, (int) (event.getY() - lastY));
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mScroller.fling(0, (int) event.getY(), 0,
                        (int) mTracker.getYVelocity(), 0, 0, 0, 100000);
                recycleVelocityTracker();
                invalidate();
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            currentY = mScroller.getCurrY();
            scrollTo(0, currentY);
            invalidate();
        } else {
            for (int i = 0; i < mAdapter.getItemCount(); i++) {
                View v = mAdapter.onCreateHolder(this, mAdapter.getItemViewType(i)).itemView;
            }
        }
    }

    public void initOrResetVelocityTracker() {
        if (mTracker == null) {
            mTracker = VelocityTracker.obtain();
        } else {
            mTracker.clear();
        }
    }

    public void initVelocityTrackerIfNeeded() {
        if (mTracker == null) {
            mTracker = VelocityTracker.obtain();
        }
    }

    public void recycleVelocityTracker() {
        if (mTracker != null) {
            mTracker.recycle();
            mTracker = null;
        }
    }


    private void notifyChanged() {
        removeAllViews();
        scrollTo(0, 0);
    }

    public static abstract class BaseAdapter<VH extends BaseHolder> {

        private WeakReference<CircularView> parentView;

        private void registerView(CircularView view) {
            parentView = new WeakReference<>(view);
        }

        protected abstract VH onCreateHolder(ViewGroup parent, int type);

        protected abstract void onBindHolder(VH holder, int position, int type);

        protected abstract int getItemCount();

        protected int getItemViewType(int position) {
            return 0;
        }

        protected void notifyDataSetChanged(){
            if (parentView != null && parentView.get() != null) {
                parentView.get().notifyChanged();
            }
        }
    }

    public static abstract class BaseHolder {

        private int type;
        protected View itemView;

        public BaseHolder(View itemView) {
            this.itemView = itemView;
        }

        public boolean isSameType(int oType) {
            return type != TYPE_NONE && type == oType;
        }
    }
}
