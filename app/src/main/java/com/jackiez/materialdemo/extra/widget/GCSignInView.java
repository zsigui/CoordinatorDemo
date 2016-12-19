package com.jackiez.materialdemo.extra.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by zsigui on 16-12-19.
 */

public class GCSignInView extends ScrollView {

    private ImageView ivIcon;
    private TextView tvName;
    private TextView tvTotalSignInCount;
    private TextView tvDate;
    private GCMonthDateView mdvContent;
    private Button btnSignIn;
    private ImageView ivAwardTitleBg;
    private ImageView ivRuleTitleBg;
    private TextView tvAwardRule;

    public GCSignInView(Context context) {
        this(context, null);
    }

    public GCSignInView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GCSignInView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


}
