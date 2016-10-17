package com.jackiez.materialdemo.extra.widget;

/**
 * Created by zsigui on 16-10-17.
 */


import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;

import com.jackiez.materialdemo.R;

/**
 * 继承自SwipeRefreshLayout,从而实现滑动到底部时上拉加载更多的功能.
 *
 * @author mrsimple
 */
public class RefreshLayout extends SwipeRefreshLayout {

    /**
     * 滑动到最下面时的上拉操作
     */

    private final static int TOUCH_SLOP = 50;
    /**
     * content view实例
     */
    private ListView mListView;
    private RecyclerView mRecyclerView;

    /**
     * 上拉监听器, 到了最底部的上拉加载操作
     */
    private OnLoadMoreListener mOnLoadListener;

    /**
     * ListView的加载中footer
     */
    private View mViewFooter;

    /**
     * 按下时的y坐标
     */
    private int mDownY;
    /**
     * 抬起时的y坐标, 与mYDown一起用于滑动到底部时判断是上拉还是下拉
     */
    private int mLastY;
    /**
     * 是否在加载中 ( 上拉加载更多 )
     */
    private boolean isLoading = false;
    /**
     * 是否显示加载更多
     */
    private boolean mCanShowLoad = true;

    /**
     * @param context
     */
    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setViewFooter(R.layout.item_footer);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // 初始化ListView对象
        if (mListView == null && mRecyclerView == null) {
            getListView();
        }
    }

    public void setViewFooter(View viewFooter) {
        mViewFooter = viewFooter;
    }

    public void setViewFooter(@LayoutRes int layoutId) {
        setViewFooter(LayoutInflater.from(getContext()).inflate(layoutId, null));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setColorSchemeResources(android.R.color.holo_red_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_blue_light,
                android.R.color.holo_green_light);
        setProgressViewOffset(true, 20, 120);
    }

    /**
     * 获取List View对象
     */
    private void getListView() {

        int childCount = getChildCount();
        if (childCount > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                View childView = getChildAt(i);
                if (childView != null) {
                    if (childView instanceof ListView) {
                        mListView = (ListView) childView;
                        // 设置滚动监听器给ListView, 使得滚动的情况下也可以自动加载
                        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                            @Override
                            public void onScrollStateChanged(AbsListView view, int scrollState) {
                                if (scrollState == SCROLL_STATE_IDLE
                                        && canLoad()) {
                                    // 滚动时到了最底部也可以加载更多
                                    loadData();
                                }
                            }

                            @Override
                            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                                 int totalItemCount) {
                            }
                        });
                        return;
                    }
                    if (childView instanceof RecyclerView) {
                        mRecyclerView = (RecyclerView) childView;
                        Log.d("tag", "getListView is Called");
                        wrapperRVAdapter(mRecyclerView);
                        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                            @Override
                            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                                if (newState == RecyclerView.SCROLL_STATE_IDLE
                                        && canLoad()) {
                                    loadData();
                                }
                            }
                        });
                        return;
                    }
                }
            }

        }
    }

    public boolean isCanShowLoad() {
        return mCanShowLoad;
    }

    public void setCanShowLoad(boolean canShowLoad) {
        mCanShowLoad = canShowLoad;
    }

    /*
         * (non-Javadoc)
         * @see android.view.ViewGroup#dispatchTouchEvent(android.view.MotionEvent)
         */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 按下
                mDownY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                mLastY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastY = (int) ev.getY();
                // 抬起
                if (canLoad()) {
                    loadData();
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 是否可以加载更多, 条件是到了最底部, listview不在加载中, 且为上拉操作.
     *
     * @return
     */
    private boolean canLoad() {
        return isCanShowLoad() && !isLoading && isBottom() && isPullUp();
    }

    /**
     * 判断是否到了最底部
     */
    private boolean isBottom() {
        if (mListView != null && mListView.getAdapter() != null) {
            return isListViewBottom();
        } else if (mRecyclerView != null && mRecyclerView.getLayoutManager() != null) {
            return isRecyclerViewBottom();
        }
        return false;
    }

    private boolean isRecyclerViewBottom() {
        Log.d("tag", "isRecyclerViewBottom is Called");
        wrapperRVAdapter(mRecyclerView);

        int lastItemPos = mRecyclerView.getAdapter().getItemCount() - 1;
        if (mRecyclerView.getLayoutManager() instanceof LinearLayoutManager
                || mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            int lastVisiblePosition;
            if (mRecyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                lastVisiblePosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                        .findLastVisibleItemPosition();
            } else {
                lastVisiblePosition = ((GridLayoutManager) mRecyclerView.getLayoutManager())
                        .findLastVisibleItemPosition();
            }
            return lastVisiblePosition >= lastItemPos;

        } else if (mRecyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = ((StaggeredGridLayoutManager) mRecyclerView.getLayoutManager());
            int[] lastPoss = new int[layoutManager.getSpanCount()];
            layoutManager.findLastVisibleItemPositions(lastPoss);
            for (int pos : lastPoss) {
                if (pos >= lastItemPos) {
                    return true;
                }
            }
        } else {
            View lastChildView = mRecyclerView.getLayoutManager().findViewByPosition(lastItemPos);
            if (lastChildView != null) {
                return mRecyclerView.getLayoutManager().getPosition(lastChildView) >= lastItemPos;
            }
        }
        return false;
    }

    private boolean isListViewBottom() {
        return mListView.getLastVisiblePosition() == (mListView.getAdapter().getCount() - 1);
    }

    /**
     * 判断并为RecyclerView的Adapter进行LoadMoreAdapter装饰
     */
    private void wrapperRVAdapter(RecyclerView recyclerView) {
        if (recyclerView != null && recyclerView.getAdapter() != null
                && !(recyclerView.getAdapter() instanceof LoadMoreAdapter)) {
            Log.d("tag", "wrapperRVAdapter is called!");
            recyclerView.setAdapter(new LoadMoreAdapter(recyclerView.getAdapter()));
            // 注册相同监听器
            recyclerView.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {});
        }
    }

    /**
     * 是否是上拉操作
     *
     * @return
     */
    private boolean isPullUp() {
        return (mDownY - mLastY) >= TOUCH_SLOP;
    }

    /**
     * 如果到了最底部,而且是上拉操作.那么执行onLoad方法
     */
    private void loadData() {
        if (mOnLoadListener != null) {
            // 设置状态
            setLoading(true);
            mOnLoadListener.onLoadMore();
        }
    }

    /**
     * @param loading
     */
    public void setLoading(boolean loading) {
        if (isLoading == loading) {
            return;
        }
        isLoading = loading;
        if (mListView != null && mListView.getAdapter() != null) {
            setListViewLoading(isLoading);
        } else if (mRecyclerView != null && mRecyclerView.getAdapter() != null) {
            setRecyclerViewLoading(isLoading);
        }
        mDownY = mLastY = 0;
    }

    private void setRecyclerViewLoading(boolean isLoading) {
        Log.d("tag", "setRecyclerViewLoading is Called");
        wrapperRVAdapter(mRecyclerView);
        if (isLoading) {
            mRecyclerView.getAdapter().notifyItemInserted(mRecyclerView.getAdapter().getItemCount() - 1);
        } else {
            mRecyclerView.getAdapter().notifyItemRemoved(mRecyclerView.getAdapter().getItemCount());
        }
    }

    private void setListViewLoading(boolean isLoading) {
        if (isLoading) {
            if (mListView.getFooterViewsCount() == 0) {
                mListView.addFooterView(mViewFooter);
            } else {
                mViewFooter.setVisibility(View.VISIBLE);
            }
        } else {
            if (mListView.getAdapter() instanceof HeaderViewListAdapter) {
                mListView.removeFooterView(mViewFooter);
            } else {
                mViewFooter.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 设置滑动到底部加载更多时的回调函数
     */
    public void setOnLoadListener(OnLoadMoreListener loadListener) {
        mOnLoadListener = loadListener;
    }

    /**
     * 加载更多适配器，用于封装 RecyclerView
     */
    private class LoadMoreAdapter extends RecyclerView.Adapter {

        private static final int TYPE_FOOTER = 0xF00F12AB;

        private RecyclerView.Adapter mAdapter;

        LoadMoreAdapter(RecyclerView.Adapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
            super.registerAdapterDataObserver(observer);
            mAdapter.registerAdapterDataObserver(observer);
        }

        @Override
        public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
            super.unregisterAdapterDataObserver(observer);
            mAdapter.unregisterAdapterDataObserver(observer);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_FOOTER) {
                return mViewFooter == null ? null : new LoadMoreHolder(mViewFooter);
            }
            return mAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) != TYPE_FOOTER) {
                mAdapter.onBindViewHolder(holder, position);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return isLoading && position == getItemCount() - 1 ?
                    TYPE_FOOTER : super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            return mAdapter.getItemCount() + (isLoading ? 1 : 0);
        }
    }

    /**
     * RecyclerView 底部加载更多视图 ViewHolder
     */
    private static class LoadMoreHolder extends RecyclerView.ViewHolder {

        private LoadMoreHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * 加载更多接口
     */
    public interface OnLoadMoreListener {
        /**
         * 当加载更多时会调用该类
         */
        void onLoadMore();
    }
}

