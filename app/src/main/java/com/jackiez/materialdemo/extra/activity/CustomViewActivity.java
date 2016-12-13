package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.jackiez.materialdemo.R;

/**
 * Created by zsigui on 16-12-13.
 */

public class CustomViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_view);
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, CustomViewActivity.class));
    }
}
