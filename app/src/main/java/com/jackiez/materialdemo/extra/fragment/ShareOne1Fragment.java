package com.jackiez.materialdemo.extra.fragment;

import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.databinding.FragmentShareOne1Binding;

/**
 * Created by zsigui on 16-10-9.
 */

public class ShareOne1Fragment extends Fragment{


    private FragmentShareOne1Binding mBinding;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        if (mBinding == null) {
            mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_share_one_1, container, false);
        }
        return mBinding.getRoot();
    }

    public View getShareIv(){
        return mBinding.ivItem;
    }

    public View getShareCv() {
        return mBinding.cvCard;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public String getShareIvName() {
        return mBinding.ivItem.getTransitionName();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public String getShareCvName() {
        return mBinding.cvCard.getTransitionName();
    }

    public static ShareOne1Fragment newInstance() {
        return new ShareOne1Fragment();
    }
}
