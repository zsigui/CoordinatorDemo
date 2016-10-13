package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.transition.Scene;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewTreeObserver;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.databinding.AcitivtyShareFragmentBinding;

/**
 * Created by zsigui on 16-10-9.
 */

public class AnimationActivity extends AppCompatActivity implements View.OnClickListener {

    private AcitivtyShareFragmentBinding mBinding;

    private Scene mScene0;
    private Scene mScene1;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.acitivty_share_fragment);

        setupLayout();
        setupWindowAnimations();
        mBinding.btn1.setOnClickListener(this);
        mBinding.btn2.setOnClickListener(this);

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupLayout() {
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {


            @Override
            public void onGlobalLayout() {
                getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                TransitionManager.go(mScene0);
            }
        });
//        getWindow().setEnterTransition(TransitionInflater.from(this)
//                .inflateTransition(R.transition.slide_changebound));
//        getWindow().getEnterTransition().addListener(new Transition.TransitionListener() {
//            @Override
//            public void onTransitionStart(Transition transition) {
//
//            }
//
//            @Override
//            public void onTransitionEnd(Transition transition) {
//                getWindow().getEnterTransition().removeListener(this);
//                TransitionManager.go(mScene0);
//            }
//
//            @Override
//            public void onTransitionCancel(Transition transition) {
//
//            }
//
//            @Override
//            public void onTransitionPause(Transition transition) {
//
//            }
//
//            @Override
//            public void onTransitionResume(Transition transition) {
//
//            }
//        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setupWindowAnimations() {
        mScene0 = Scene.getSceneForLayout(mBinding.flPos1, R.layout.view_scene_animation_1, this);
        mScene1 = Scene.getSceneForLayout(mBinding.flPos1, R.layout.view_scene_animation_2, this);
        mScene0.setEnterAction(new Runnable() {
            @Override
            public void run() {
                animateButton(mBinding.btn1, 0);
                animateButton(mBinding.btn2, 100);
            }
        });
        mBinding.btn1.setScaleX(0);
        mBinding.btn1.setScaleY(0);
        mBinding.btn2.setScaleX(0);
        mBinding.btn2.setScaleY(0);
        mBinding.btn1.setText("类型1");
        mBinding.btn2.setText("类型2");
    }

    private void animateButton(View v, int delay) {
        v.animate()
                .setStartDelay(delay)
                .scaleX(1)
                .scaleY(1);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_1:
                TransitionManager.go(mScene0,
                        TransitionInflater.from(this).inflateTransition(R.transition.slide_changebound));
                break;
            case R.id.btn_2:
                TransitionManager.go(mScene1,
                        TransitionInflater.from(this).inflateTransition(R.transition
                                .slide_changebound_with_sequential));
                break;
        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, AnimationActivity.class);
        context.startActivity(intent);
    }
}
