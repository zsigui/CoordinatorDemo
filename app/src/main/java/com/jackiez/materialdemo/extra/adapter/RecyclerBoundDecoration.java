package com.jackiez.materialdemo.extra.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.jackiez.materialdemo.R;

/**
 * Created by zsigui on 16-10-17.
 */
public class RecyclerBoundDecoration extends RecyclerView.ItemDecoration {

    private int mInsets;

    public RecyclerBoundDecoration(Context context) {
        mInsets = context.getResources().getDimensionPixelSize(R.dimen.di_bound_size);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

        //We can supply forced insets for each item view here in the Rect
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(mInsets, mInsets, mInsets, mInsets);
    }
}