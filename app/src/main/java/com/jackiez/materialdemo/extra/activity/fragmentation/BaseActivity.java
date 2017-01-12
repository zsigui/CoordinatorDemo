//package com.jackiez.materialdemo.extra.activity.fragmentation;
//
//import android.os.Bundle;
//import android.support.annotation.Nullable;
//
//import com.jackiez.materialdemo.R;
//
//import me.yokeyword.fragmentation.SupportActivity;
//import me.yokeyword.fragmentation.SupportFragment;
//
///**
// * Created by zsigui on 17-1-5.
// */
//
//public class BaseActivity extends SupportActivity {
//
//    private static final String KEY_INDEX = "key_index";
//    private SupportFragment[] mFragments;
//    private int mCurrentIndex = 0;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_fragmentation);
//        mFragments = new SupportFragment[3];
//        if (savedInstanceState != null) {
//            mFragments[0] = new HomeFragment();
//            mFragments[1] = new FriendFragment();
//            mFragments[2] = new SettingFragment();
//            loadMultipleRootFragment(R.id.fl_container,
//                    mCurrentIndex,
//                    mFragments[0],
//                    mFragments[1],
//                    mFragments[2]);
//        } else {
//            mFragments[0] = findFragment(HomeFragment.class);
//            mFragments[1] = findFragment(FriendFragment.class);
//            mFragments[2] = findFragment(SettingFragment.class);
//        }
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        mCurrentIndex = savedInstanceState.getInt(KEY_INDEX, 0);
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putInt(KEY_INDEX, 0);
//    }
//}
