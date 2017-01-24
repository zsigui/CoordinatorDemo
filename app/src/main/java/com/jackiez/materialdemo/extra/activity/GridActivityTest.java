package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.widget.FrameLayout;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.databinding.ActivityGridBinding;
import com.jackiez.materialdemo.extra.adapter.PicTextAdapter;
import com.jackiez.materialdemo.extra.adapter.RecyclerBoundDecoration;
import com.jackiez.materialdemo.extra.utils.UIUtil;

import java.util.ArrayList;

/**
 * Created by zsigui on 16-10-17.
 */

public class GridActivityTest extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {


    ActivityGridBinding mBinding;
    private ArrayList<String> mData;
    PicTextAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_grid);

        setSupportActionBar(mBinding.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mBinding.drawer, mBinding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mBinding.drawer.setDrawerListener(toggle);
        toggle.syncState();


        UIUtil.setTranslucentStatusBar(this);
        mBinding.ablBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                mBinding.splash.setEnabled(verticalOffset >= 0);
            }
        });
        mData = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            mData.add("testAddApkDownloadInfo " + i);
        }
        mBinding.swlCoantener.setOnRefreshListener(this);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mBinding.toolbar.getLayoutParams();
        lp.topMargin = UIUtil.getStatusBarSize(this);
        mBinding.toolbar.setLayoutParams(lp);

        adapter = new PicTextAdapter(this);
        adapter.updateData(mData);

        mBinding.rvContent.setAdapter(adapter);
        mBinding.rvContent.addItemDecoration(new RecyclerBoundDecoration(this));
        mBinding.rvContent.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false));

//        mBinding.flCon.removeAllViews();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mBinding.flCon.removeAllViews();
//                mBinding.flCon.addView(mBinding.swlCoantener);
//            }
//        }, 3000);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, GridActivityTest.class);
        context.startActivity(intent);
    }

    @Override
    public void onRefresh() {
        for (int i = 0; i < 20; i++) {
            mData.add("newAdd " + i);
        }
        adapter.addMore(20);
        if (mBinding.swlCoantener.isRefreshing())
            mBinding.swlCoantener.setRefreshing(false);
    }
}
