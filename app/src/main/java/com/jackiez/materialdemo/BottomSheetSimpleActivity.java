package com.jackiez.materialdemo;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.jackiez.materialdemo.databinding.ActivityBottomSheetSimpleBinding;

public class BottomSheetSimpleActivity extends AppCompatActivity {

    private ActivityBottomSheetSimpleBinding mBinding;
    int statusBarSize;
    int screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statusBarSize = 72;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_bottom_sheet_simple);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(mBinding.llBottom);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//                mBinding.llBottomBar.setTranslationY(-statusBarSize * (1.0f - slideOffset));
            }
        });
//        mBinding.llBottomBar.setTranslationY(-statusBarSize);
    }
}
