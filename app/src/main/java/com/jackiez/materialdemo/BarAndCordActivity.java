package com.jackiez.materialdemo;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;

import com.jackiez.materialdemo.databinding.ActivityBarAndCordBinding;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2016/9/30
 */

public class BarAndCordActivity extends AppCompatActivity {

    private ActivityBarAndCordBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_bar_and_cord);
        setSupportActionBar(mBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("CoordinatorLayout简单示例");
        }
        mBinding.vpContent.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public CharSequence getPageTitle(int position) {
                return "Tab" + position;
            }

            @Override
            public Fragment getItem(int position) {
                return new PageFragment();
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
        // 方法一
//        mBinding.tab.addTab(mBinding.tab.newTab().setText("Tab3"));
//        mBinding.tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
//            @Override
//            public void onTabSelected(TabLayout.Tab tab) {
//                mBinding.vpContent.setCurrentItem(mBinding.tab.getSelectedTabPosition(), true);
//            }
//
//            @Override
//            public void onTabUnselected(TabLayout.Tab tab) {
//
//            }
//
//            @Override
//            public void onTabReselected(TabLayout.Tab tab) {
//
//            }
//        });
//        mBinding.vpContent.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//
//            }
//
//            @Override
//            public void onPageSelected(int position) {
//                mBinding.tab.getTabAt(position).select();
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int state) {
//
//            }
//        });
        // 方法二，该方法会清空之前已设置的Tab
        mBinding.tab.setupWithViewPager(mBinding.vpContent, true);
    }
}
