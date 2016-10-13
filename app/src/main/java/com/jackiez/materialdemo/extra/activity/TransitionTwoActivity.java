package com.jackiez.materialdemo.extra.activity;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.transition.Transition;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.databinding.ActivityTransitionTwoBinding;

/**
 * Created by zsigui on 16-10-8.
 */

public class TransitionTwoActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityTransitionTwoBinding mBinding;

    public void makeTheStatusbarTranslucent (Activity activity) {

        Window w = activity.getWindow();
        w.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        w.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_transition_two);
        makeTheStatusbarTranslucent(this);
        setSupportActionBar(mBinding.toolbar);
        if (getIntent() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int pos = getIntent().getIntExtra(TransitionOneActivity.EXTRA_POS_KEY,  -1);
            if (pos != -1)
                mBinding.ivTitle.setTransitionName(TransitionOneActivity.TRANS_NAME_PREFIX + pos);
            int res = getIntent().getIntExtra(TransitionOneActivity.EXTRA_KEY, 0);
            if (res != 0) {
                Bitmap bm = BitmapFactory.decodeResource(getResources(), res);
                mBinding.ivTitle.setImageBitmap(bm);
                Palette.from(bm).generate(new Palette.PaletteAsyncListener() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onGenerated(Palette palette) {
                        int color = palette.getLightVibrantColor(-1);
                        if (color != -1) {
                            mBinding.ctlContainer.setContentScrimColor(color);
                        }
                        int status_color = palette.getDarkVibrantColor(-1);
                        if (status_color != -1) {
                            getWindow().setStatusBarColor(status_color);
                            if (color == -1) {
                                mBinding.ctlContainer.setContentScrimColor(status_color);
                            }
                        }
                    }
                });
            }
            configureEnterTransition();
        }
        int statusSize = getStatusSize();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mBinding.toolbar.getLayoutParams();
        params.topMargin = statusSize;
        mBinding.toolbar.setLayoutParams(params);

    }

    private int getStatusSize() {
        return getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void configureEnterTransition() {
        postponeEnterTransition();
        mBinding.cvCard.setOnClickListener(null);
        mBinding.nsvContainer.setScaleY(0);
        startPostponedEnterTransition();
        getWindow().getSharedElementEnterTransition().addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {

            }

            @Override
            public void onTransitionEnd(Transition transition) {
                ViewPropertyAnimator animator = mBinding.nsvContainer.animate().setStartDelay(300)
                        .scaleY(1);
                animator.setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mBinding.cvCard.setOnClickListener(TransitionTwoActivity.this);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animator.start();
            }

            @Override
            public void onTransitionCancel(Transition transition) {

            }

            @Override
            public void onTransitionPause(Transition transition) {

            }

            @Override
            public void onTransitionResume(Transition transition) {

            }
        });
    }


    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            configureExitAnimation();

        } else {
            super.onBackPressed();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void configureExitAnimation() {
        int x = mBinding.nsvContainer.getWidth() / 2;
        int y = mBinding.nsvContainer.getTop() / 2;
        int startRadius = Math.max(mBinding.nsvContainer.getWidth(), mBinding.nsvContainer.getTop());
        Animator animator = ViewAnimationUtils.createCircularReveal(mBinding.nsvContainer,
                x, y, startRadius, 0);
        animator.setDuration(300);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mBinding.nsvContainer.setVisibility(View.GONE);
//                        finishAfterTransition();
                    supportFinishAfterTransition();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    @Override
    public void onClick(View v) {
        Snackbar.make(v, "ScrollView is clicked!", Snackbar.LENGTH_SHORT).show();
    }
}
