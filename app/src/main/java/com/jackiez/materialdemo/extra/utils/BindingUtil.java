package com.jackiez.materialdemo.extra.utils;

import android.databinding.BindingAdapter;
import android.databinding.repacked.google.common.base.Strings;
import android.widget.ImageView;

/**
 * Created by zsigui on 16-10-17.
 */

public class BindingUtil {


    @BindingAdapter({"image"})
    public static void loadImage(ImageView iv, String url) {
        if (iv == null) return;
        if (Strings.isNullOrEmpty(url)) return;
//        url = RestConst.BASE_PICS_SRC + url;
//        Glide.with(iv.getContext())
//                .load(url)
//                .placeholder(R.drawable.awesome)
//                .error(android.R.drawable.ic_delete)
//                .centerCrop()
//                .crossFade()
//                .into(iv);
//        iv.setImageURI(url);
    }
}
