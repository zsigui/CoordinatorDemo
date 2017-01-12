/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.jackiez.materialdemo.extra.activity;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.jackiez.materialdemo.R;
import com.jackiez.materialdemo.extra.dialog.BottomSheetDialog;
import com.luna.powersaver.gp.GuardService;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String GITHUB_REPO_URL = "https://github.com/saulmm/CoordinatorExamples";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GuardService.testAliveAndCreateIfNot(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL));
                startActivity(browserIntent);
                return true;
            }
        });

        findViewById(R.id.main_coordinator_tablayout).setOnClickListener(this);
        findViewById(R.id.main_coordinator_textview).setOnClickListener(this);
        findViewById(R.id.main_materialup_textview).setOnClickListener(this);
        findViewById(R.id.main_ioexample_textview).setOnClickListener(this);
        findViewById(R.id.main_space_textview).setOnClickListener(this);
        findViewById(R.id.main_swipebehavior_textview).setOnClickListener(this);
        findViewById(R.id.main_materialup_customebevior).setOnClickListener(this);
        findViewById(R.id.main_materialup_bottomsheetbehavior).setOnClickListener(this);
        findViewById(R.id.main_materialup_bottomsheetdialog).setOnClickListener(this);
        findViewById(R.id.main_transition_shareelement).setOnClickListener(this);
        findViewById(R.id.main_fragment_shareelement).setOnClickListener(this);
        findViewById(R.id.main_animation_scene).setOnClickListener(this);
        findViewById(R.id.main_test).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_coordinator_tablayout:
                TabActivity.start(this);
                break;

            case R.id.main_coordinator_textview:
                SimpleCoordinatorActivity.start(this);
                break;

            case R.id.main_ioexample_textview:
                IOActivityExample.start(this);
                break;

            case R.id.main_space_textview:
                FlexibleSpaceExampleActivity.start(this);
                break;

            case R.id.main_materialup_textview:
                MaterialUpConceptActivity.start(this);
                break;

            case R.id.main_swipebehavior_textview:
                SwipeBehaviorExampleActivity.start(this);
                break;

            case R.id.main_materialup_customebevior:
                CustomBehaviorActivity.start(this);
                break;

            case R.id.main_materialup_bottomsheetbehavior:
                BottomSheetActivity.start(this);
                break;

            case R.id.main_materialup_bottomsheetdialog:
                BottomSheetDialog dialog = new BottomSheetDialog(this);
                dialog.show();
                break;
            case R.id.main_transition_shareelement:
                TransitionOneActivity.start(this);
                break;
            case R.id.main_fragment_shareelement:
                ShareFragmentActivity.start(this);
                break;
            case R.id.main_animation_scene:
                AnimationActivity.start(this);
                break;
            case R.id.main_test:
                CustomViewActivity.start(this);
                break;
            case R.id.main_test_accessibility:
                CheckIfUseAccessiableService();
                break;
        }
    }

    private void CheckIfUseAccessiableService() {
        if (!isAccessibleEnabled()) {
            startActivity(new Intent("android.settings.ACCESSIBILITY_SETTINGS"));
        }
    }

    private boolean isAccessibleEnabled() {
        List<AccessibilityServiceInfo> infos = ((AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE))
                .getEnabledAccessibilityServiceList(-1);
        final String service = getPackageName() + "/.AppLocker.Service.MyAccessibilityService";
        for (AccessibilityServiceInfo info : infos) {
            Log.i("test-test", "info = " + info.getId() + ", service = " + service);
            if (info.getId().equals(service)) {
                return true;
            }
        }
        return false;
    }
}
