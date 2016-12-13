package com.jackiez.materialdemo.extra.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by zsigui on 16-10-17.
 */

public class UIUtil {


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void setTranslucentStatusBar(Activity activity) {
        if (activity == null) return;
        Window w = activity.getWindow();
        w.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        w.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    public static int getStatusBarSize(Context context) {
        return context.getResources().getDimensionPixelSize(
                context.getResources().getIdentifier("status_bar_height", "dimen", "android"));
    }

    /**
     * 获取更深的颜色
     */
    public static int getDarkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = hsv[1] + 0.1f; // 饱和度提高
        hsv[2] = hsv[2] - 0.1f; // 对比度降低
        return Color.HSVToColor(hsv);
    }

    /**
     * 获取更浅的颜色
     */
    public static int getBrighterColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = hsv[1] - 0.1f; // 饱和度降低
        hsv[2] = hsv[2] + 0.1f; // 对比度提高
        return Color.HSVToColor(hsv);
    }
}
