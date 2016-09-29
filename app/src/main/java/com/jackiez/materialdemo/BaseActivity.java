package com.jackiez.materialdemo;

import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2016/9/27
 */

public abstract class BaseActivity<Binding extends ViewDataBinding, Data> extends AppCompatActivity {

    protected Binding mBinding;

    protected Data mData;

    abstract void initBinding();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBinding();
        processLogic();
        setListener();
    }

    protected abstract void processLogic();

    protected abstract void setListener();

}
