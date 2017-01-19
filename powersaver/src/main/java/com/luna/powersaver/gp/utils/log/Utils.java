package com.luna.powersaver.gp.utils.log;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * Created by zsigui on 16-9-29.
 */
public class Utils {


    public static String getOwnCacheDirectory(Context context, String cacheDir, boolean preferExternal) {
        File appCacheDir = null;
        if (preferExternal && MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                && isWithPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            appCacheDir = new File(Environment.getExternalStorageDirectory(), cacheDir);
        }
        if (appCacheDir == null || (!appCacheDir.exists() && !appCacheDir.mkdirs())) {
            appCacheDir = context.getCacheDir();
        }
        return (appCacheDir == null ? "" : appCacheDir.getAbsolutePath());
    }

    public static boolean isWithPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_DENIED;
    }

    public static void printFile(String directory, String fileName, String content, boolean append) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(directory, fileName), append);
            fw.write(content);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
