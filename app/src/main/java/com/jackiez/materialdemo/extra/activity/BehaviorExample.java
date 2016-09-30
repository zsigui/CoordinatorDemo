package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class BehaviorExample extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.jackiez.materialdemo.R.layout.activity_behavior);
	}

	public static void start (Context context) {
		context.startActivity(new Intent(context, BehaviorExample.class));
	}
}
