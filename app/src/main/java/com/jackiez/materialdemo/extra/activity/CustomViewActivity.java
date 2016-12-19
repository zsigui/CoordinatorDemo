package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.extra.widget.MonthDateView;

/**
 * Created by zsigui on 16-12-13.
 */

public class CustomViewActivity extends AppCompatActivity {

    MonthDateView mdv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_view);
        mdv = (MonthDateView) findViewById(R.id.mdv_content);
        mdv.setItemTapListener(new MonthDateView.onItemTapListener() {
            @Override
            public void onItemTap(int index) {
                Toast.makeText(CustomViewActivity.this, "单击事件 : " + index,  Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemLongTap(int index) {
                Toast.makeText(CustomViewActivity.this, "长按事件 : " + index,  Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onItemDoubleTap(int index) {
                Toast.makeText(CustomViewActivity.this, "双击事件 : " + index,  Toast.LENGTH_SHORT).show();

            }
        });
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, CustomViewActivity.class));
    }
}
