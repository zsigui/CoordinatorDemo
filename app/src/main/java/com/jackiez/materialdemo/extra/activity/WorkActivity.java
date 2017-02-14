package com.jackiez.materialdemo.extra.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.jackiez.materialdemo.R;
import com.luna.powersaver.gp.PowerSaver;
import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.manager.StalkerManager;
import com.luna.powersaver.gp.utils.AppUtil;
import com.luna.powersaver.gp.utils.FileUtil;

import java.io.File;

/**
 * Created by zsigui on 17-1-24.
 */

public class WorkActivity extends AppCompatActivity implements View.OnClickListener {

//    public static TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_work);

        findViewById(R.id.add_apk_download).setOnClickListener(this);
        findViewById(R.id.add_gp_download).setOnClickListener(this);
        findViewById(R.id.clear_data).setOnClickListener(this);
        findViewById(R.id.jump_set_accessibility).setOnClickListener(this);
        findViewById(R.id.wake_guard).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        tv = null;
    }

    @Override
    public void onClick(View v) {
        String s;
        switch (v.getId()) {
            case R.id.add_apk_download:
                s = StalkerManager.get().testAddApkDownloadInfo();
                Toast.makeText(this, "Download任务: " + s, Toast.LENGTH_SHORT).show();
                break;
            case R.id.add_gp_download:
                s = StalkerManager.get().testAddGpDownloadInfo();
                Toast.makeText(this, "GP任务: " + s, Toast.LENGTH_SHORT).show();
                break;
            case R.id.clear_data:
                File tempFile = FileUtil.getOwnCacheDirectory(StaticConst.sContext, StaticConst.STORE_DATA);
                tempFile = new File(tempFile, "all");
                FileUtil.delete(tempFile);
                tempFile = new File(tempFile, "k");
                FileUtil.delete(tempFile);
                Toast.makeText(this, "清除记录数据成功", Toast.LENGTH_SHORT).show();
                break;
            case R.id.jump_set_accessibility:
                checkIfUseAccessiableService(this);
                break;
            case R.id.wake_guard:
                PowerSaver.init(this);
                PowerSaver.get().showGuardView(this);
                break;
        }
    }


    public void checkIfUseAccessiableService(Context context) {
        if (!AppUtil.isAccessibleEnabled(context)) {
            context.startActivity(new Intent("android.settings.ACCESSIBILITY_SETTINGS"));
        } else {
            Toast.makeText(context, "已经设置了无障碍权限", Toast.LENGTH_SHORT).show();
        }
    }
}