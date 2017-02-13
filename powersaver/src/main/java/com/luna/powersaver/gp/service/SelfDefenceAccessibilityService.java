package com.luna.powersaver.gp.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

import com.luna.powersaver.gp.utils.AppDebugLog;

/**
 * Created by zsigui on 17-2-13.
 */

public class SelfDefenceAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {
        AppDebugLog.d(AppDebugLog.TAG_ACCESSIBILITY, "SelfDefenceAccessibilityService.onInterrupt!");
    }
}
