package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.databinding.ActivityTabBinding;
import com.jackiez.materialdemo.extra.fragment.MaterialUpConceptFakePage;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2016/9/30
 */

public class TabActivity extends AppCompatActivity {

    private ActivityTabBinding mBinding;

    private String[] tabTitles = {"Tab1", "Tab2", "Tab3"};
    private int[] imageResId = {android.R.drawable.ic_menu_camera,
            android.R.drawable.ic_menu_call,
            android.R.drawable.ic_menu_compass};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_tab);
        setSupportActionBar(mBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("CoordinatorLayout简单示例");
        }
        PagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public CharSequence getPageTitle(int position) {
                // 使用 Spannable 的方式为 Tab 添加图文
                // 使用这种方式需要设置 style 里的 textAllCaps 属性才能显示 Spannable效果，具体见下面
//                Drawable image = context.getResources().getDrawable(imageResId[position]);
//                image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
//                SpannableString sb = new SpannableString(" " + tabTitles[position]);
//                ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
//                sb.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return "Tab" + position;
            }

            @Override
            public Fragment getItem(int position) {
                return new MaterialUpConceptFakePage();
            }

            @Override
            public int getCount() {
                return 3;
            }

        };
        mBinding.vpContent.setAdapter(adapter);
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
        mBinding.tab.setTabMode(TabLayout.MODE_FIXED);
        for (int i = 0; i < mBinding.tab.getTabCount(); i++) {
            TabLayout.Tab tab = mBinding.tab.getTabAt(i);
            tab.setCustomView(getTabView(i));
        }
    }

    public View getTabView(int position){
        // 给 Tab 设置自定义
        View view = LayoutInflater.from(TabActivity.this).inflate(R.layout.tab_item, null);
        TextView tv= (TextView) view.findViewById(R.id.textView);
        tv.setText(tabTitles[position]);
        ImageView img = (ImageView) view.findViewById(R.id.imageView);
        img.setImageResource(imageResId[position]);
        return view;
    }

    public static void start(Context c) {
        c.startActivity(new Intent(c, TabActivity.class));
    }
}
