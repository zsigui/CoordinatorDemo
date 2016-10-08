package com.jackiez.materialdemo.extra.listener;

import android.view.View;

/**
 * Created by zsigui on 16-10-8.
 */

public interface OnItemClickListener<T> {

    void onItemClick(View v, int pos, T data);
}
