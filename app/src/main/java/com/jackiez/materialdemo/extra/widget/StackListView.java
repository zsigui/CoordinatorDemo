package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Created by zsigui on 17-1-16.
 */

public class StackListView extends ViewGroup{

    public StackListView(Context context) {
        this(context, null);
    }

    public StackListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }
}
