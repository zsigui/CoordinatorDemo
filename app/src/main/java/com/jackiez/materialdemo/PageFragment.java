package com.jackiez.materialdemo;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jackiez.materialdemo.databinding.FragmentPageBinding;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2016/9/30
 */

public class PageFragment extends Fragment {

    private FragmentPageBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mBinding == null) {
            mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_page, container, false);
        }
        return mBinding.getRoot();
    }
}
