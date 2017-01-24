package com.jackiez.materialdemo.extra;

import android.app.Application;

import com.luna.powersaver.gp.PowerSaver;

/**
 * Created by zsigui on 17-1-24.
 */

public class DefaultApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PowerSaver.init(this);
    }
}
