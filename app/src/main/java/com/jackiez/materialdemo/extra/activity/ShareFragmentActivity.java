package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.databinding.AcitivtyShareFragmentBinding;
import com.jackiez.materialdemo.extra.fragment.ShareOne1Fragment;
import com.jackiez.materialdemo.extra.fragment.ShareOne2Fragment;

/**
 * Created by zsigui on 16-10-9.
 */

public class ShareFragmentActivity extends AppCompatActivity implements View.OnClickListener {


    private boolean mOneClicked = true;
    private boolean mTwoClicked = false;
    private AcitivtyShareFragmentBinding mBinding;
    private Fragment mOneFragment;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.acitivty_share_fragment);
        mBinding.btn1.setOnClickListener(this);

        mOneFragment = ShareOne1Fragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_pos_1, mOneFragment)
                .commit();
        mOneClicked = true;
        mBinding.btn1.setText("Replace Two");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void replaceOneFirstFragment() {
        ShareOne1Fragment fragment = ShareOne1Fragment.newInstance();

        ShareOne2Fragment old = (ShareOne2Fragment) mOneFragment;

        ChangeBounds changeTransform = new ChangeBounds();
        // 设置新的共享进入动画
        fragment.setSharedElementEnterTransition(changeTransform);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_pos_1, fragment)
                .addSharedElement(old.getShareIv(), old.getShareIvName())
                .addSharedElement(old.getShareCv(), old.getShareCvName())
                .commit();
        mBinding.btn1.setText("Replace Two");
        mOneClicked = true;
        mOneFragment = fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void replaceOneSecondFragment() {
        ShareOne2Fragment fragment = ShareOne2Fragment.newInstance();

        ShareOne1Fragment old = (ShareOne1Fragment) mOneFragment;

        ChangeBounds changeTransform = new ChangeBounds();
        fragment.setSharedElementEnterTransition(changeTransform);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_pos_1, fragment)
                .addSharedElement(old.getShareIv(), old.getShareIvName())
                .addSharedElement(old.getShareCv(), old.getShareCvName())
                .commit();
        mBinding.btn1.setText("Replace One");
        mOneClicked = false;
        mOneFragment = fragment;
        TransitionManager.beginDelayedTransition((ViewGroup) mBinding.getRoot());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_1:
                if (mOneClicked) {
                    replaceOneSecondFragment();
                } else {
                    replaceOneFirstFragment();
                }
                break;
            case R.id.btn_2:
                break;
        }
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, ShareFragmentActivity.class));
    }
}
