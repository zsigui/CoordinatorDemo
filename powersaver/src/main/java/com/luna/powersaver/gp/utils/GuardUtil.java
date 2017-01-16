package com.luna.powersaver.gp.utils;

import android.app.KeyguardManager;
import android.content.Context;

/**
 * Created by zsigui on 17-1-16.
 */

public class GuardUtil {


    public static void openSystemGuard(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock("");
        lock.reenableKeyguard();
    }

    public static void closeSystemGuard(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock("");
        lock.disableKeyguard();

    }
}
