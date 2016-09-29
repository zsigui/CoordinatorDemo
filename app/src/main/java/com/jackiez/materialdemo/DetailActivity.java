package com.jackiez.materialdemo;

import android.databinding.DataBindingUtil;

import com.jackiez.materialdemo.databinding.ActivityDetailBinding;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2016/9/27
 */

public class DetailActivity extends BaseActivity<ActivityDetailBinding, Object> {

    @Override
    void initBinding() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);
    }

    @Override
    protected void processLogic() {
    }

    @Override
    protected void setListener() {

    }

}
