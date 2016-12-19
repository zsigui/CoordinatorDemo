package com.jackiez.materialdemo.extra.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorRes;
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
//        float[] hsv = new float[3];
//        Color.colorToHSV(color, hsv);
//        hsv[1] = hsv[1] + 0.1f; // 饱和度提高
//        hsv[2] = hsv[2] - 0.1f; // 对比度降低
//        return Color.HSVToColor(hsv);

        int tmp = 0xFF000000 & color;
        if (tmp > 0x1A000000) {
            return color - 0x1A000000;
        } else {
            return color - tmp;
        }
    }

    /**
     * 获取更浅的颜色
     */
    public static int getBrighterColor(int color) {
//        float[] hsv = new float[3];
//        Color.colorToHSV(color, hsv);
//        hsv[1] = hsv[1] - 0.1f; // 饱和度降低
//        hsv[2] = hsv[2] + 0.1f; // 对比度提高
//        return Color.HSVToColor(hsv);
        int tmp = 0xFF000000 & color;
        if (tmp < 0xFF000000 - 0x1A000000) {
            return color + 0x1A000000;
        } else {
            return color | 0xFF000000;
        }
    }

    public static int getColor(Context context, @ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor(color, null);
        } else {
            return context.getResources().getColor(color);
        }
    }
}
