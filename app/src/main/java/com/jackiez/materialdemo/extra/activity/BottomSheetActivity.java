package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.databinding.ActivityBottomSheetBinding;

public class BottomSheetActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityBottomSheetBinding mBinding;
    boolean canBeTouch = false;

    private BottomSheetBehavior bottomSheetBehavior;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(mBinding.llBottom);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset >= 0.9f) {
                    mBinding.appbar.setTranslationY(bottomSheet.getTop() - mBinding.toolbar.getHeight());
                } else {
                    mBinding.appbar.setTranslationY(0);
                }
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_SETTLING
                        && bottomSheet.getTop() < 700 && bottomSheet.getTop() > 450) {
                    bottomSheetBehavior.setPeekHeight(bottomSheet.getHeight() - 600);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    bottomSheetBehavior.setPeekHeight(mBinding.toolbar.getHeight());
                }
                if (slideOffset >= 1.0f) {
                    mBinding.tvSlideBar.setText("BottomSheet已经全部展开");
                    canBeTouch = true;
                } else {
                    mBinding.tvSlideBar.setText("可通过此滑动唤醒BottomSheet ↑");
                    canBeTouch = false;
                }
            }
        });
        mBinding.tvSlideBar.setOnClickListener(this);
    }

    public static void start(Context c) {
        c.startActivity(new Intent(c, BottomSheetActivity.class));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_slide_bar:
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED
                        && canBeTouch) {
                    Snackbar.make(v, "BottomSheet展开后被点击", Snackbar.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
