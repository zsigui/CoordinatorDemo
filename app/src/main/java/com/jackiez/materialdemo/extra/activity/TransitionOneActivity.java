package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.transition.Explode;
import android.view.View;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.databinding.ActivityTransitionOneBinding;
import com.jackiez.materialdemo.extra.adapter.PicAdapter;
import com.jackiez.materialdemo.extra.listener.OnItemClickListener;

/**
 * Created by zsigui on 16-10-8.
 */

public class TransitionOneActivity extends AppCompatActivity implements OnItemClickListener<Object> {


    private ActivityTransitionOneBinding mBinding;

    public static final String TRANS_NAME_PREFIX = "share";
    public static final String EXTRA_KEY = "extra";
    public static final String EXTRA_POS_KEY = "extra_pos";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_transition_one);
        mBinding.rvContent.setLayoutManager(new GridLayoutManager(this, 2));
        PicAdapter adapter = new PicAdapter(this);
        mBinding.rvContent.setAdapter(adapter);
        adapter.setListener(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onItemClick(View v, int pos, Object data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            v.setTransitionName(TRANS_NAME_PREFIX + pos);
            Intent intent = new Intent(this, TransitionTwoActivity.class);
            intent.putExtra(EXTRA_KEY, (Integer) data);
            intent.putExtra(EXTRA_POS_KEY, pos);
            ActivityOptionsCompat optionsCompat = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(this, new Pair<View, String>(v, TRANS_NAME_PREFIX + pos));
            ActivityCompat.startActivity(this, intent, optionsCompat.toBundle());

        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, TransitionOneActivity.class);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            ((AppCompatActivity) context).getWindow().setEnterTransition(new Explode());
        }
        context.startActivity(intent);
    }
}
