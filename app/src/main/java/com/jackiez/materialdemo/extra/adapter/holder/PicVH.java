package com.jackiez.materialdemo.extra.adapter.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.jackiez.materialdemo.R;

/**
 * Created by zsigui on 16-10-8.
 */

public class PicVH extends RecyclerView.ViewHolder {

    public ImageView ivItem;

    public PicVH(View itemView) {
        super(itemView);
        ivItem = (ImageView) itemView.findViewById(R.id.iv_item);

    }
}
