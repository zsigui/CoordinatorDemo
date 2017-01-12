package com.jackiez.materialdemo.extra.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author JackieZhuang
 * @email zsigui@foxmail.com
 * @date 2016/12/26
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NBAccessibilityService extends AccessibilityService {

    private static final String TAG = "acs-test";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "NBAccessibilityService.onServiceConnected!");
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        Log.w(TAG, "onAccessibilityEvent is called! type = " + event.getEventType()
                + ", action = " + event.getAction() + ", time = " + df.format(new Date(event.getEventTime()))
                + ", text = " + event.getText() + ", package = " + event.getPackageName()
                + ", source.text = " + (event.getSource() != null ? event.getSource().getText() : "null"));
        traveselNodeInfo(getRootInActiveWindow(), 0);
    }

    private boolean traveselNodeInfo(AccessibilityNodeInfo info, int depth) {
        StringBuilder spaceCount = new StringBuilder();
        int d = depth;
        while (d-- > 0) {
            spaceCount.append("--");
        }
        if (info != null) {
            Log.d(TAG, spaceCount.toString() + "info.class = " + info.getClassName() + ", getChildCount = " +
                    info.getChildCount()
                    + ", label = " + info.getText() + ", packageName = " + info.getPackageName() + ", isClick = " +
                    info.isClickable() + ", isEnabled = " + info.isEnabled()
                    + ", windowId = " + info.getWindowId());
            if (info.getChildCount() != 0) {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (traveselNodeInfo(info.getChild(i), depth + 1)) {
                        info.recycle();
                        return true;
                    }
                }
                info.recycle();
            }
//            else if (info.getClassName().toString().equals("android.widget.TextView")
//                    && "setting".equals(info.getText())) {
//                DP.D(DP.TAG_TEST, "find the textview setting! to click! isClick = " + info.isClickable() + ",
// isEnabled = " + info.isEnabled());
//                final AccessibilityNodeInfo s = info.getParent();
//                DP.D(DP.TAG_TEST, "perform click successful or not = " + s.performAction(AccessibilityNodeInfo
// .ACTION_CLICK));
//                s.recycle();
//                info.recycle();
//                return true;
//            }
        } else {
            Log.d(TAG, spaceCount.toString() + "null");
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "NBAccessibilityService.onInterrupt!");
    }
}
