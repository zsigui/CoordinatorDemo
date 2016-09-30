package com.jackiez.materialdemo.extra;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.v7.widget.RecyclerView;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.extra.adapter.FakePageAdapter;

/**
 * Created by zsigui on 16-9-30.
 */

public class BottomSheetDialog extends android.support.design.widget.BottomSheetDialog {

    public BottomSheetDialog(@NonNull Context context) {
        super(context);
    }

    public BottomSheetDialog(@NonNull Context context, @StyleRes int theme) {
        super(context, theme);
    }

    protected BottomSheetDialog(@NonNull Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_page);
        RecyclerView mRootView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRootView.setAdapter(new FakePageAdapter(20));
    }
}
