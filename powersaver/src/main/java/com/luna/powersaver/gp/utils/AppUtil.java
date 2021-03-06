package com.luna.powersaver.gp.utils;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.luna.powersaver.gp.BuildConfig;
import com.luna.powersaver.gp.common.GPResId;
import com.luna.powersaver.gp.entity.JsonAppInfo;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import static android.content.Context.ACCESSIBILITY_SERVICE;

/**
 * Created by zsigui on 17-1-18.
 */

public class AppUtil {

    public static int GPVC = -1;

    public static String getAppInfo(Context context) {
        if (context == null)
            return "";
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> infos = pm.getInstalledPackages(0);
        StringBuilder builder = new StringBuilder();
        for (PackageInfo info : infos) {
            if (GPResId.PACKAGE.equals(info.packageName)) {
                GPVC = info.versionCode;
            }
            builder.append(info.packageName).append("|");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    /**
     * 跳转谷歌商店首页
     */
    public static boolean jumpToStore(Context context) {
        if (context == null)
            return false;
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(GPResId.PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    /**
     * 跳转谷歌商店特定应用的详情界面
     */
    public static boolean jumpToStore(Context context, String packageName) {
        if (context == null)
            return false;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(GPResId.getWakeAction() + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isPkgInstalled(context, GPResId.PACKAGE)) {
            intent.setClassName(GPResId.PACKAGE, GPResId.getMainActivityClassName());
            context.startActivity(intent);
            return true;
        } else {
            AppDebugLog.d("没有可处理的市场类应用");
        }
        return false;
    }

    /**
     * 跳转特定应用的应用程序信息界面
     */
    public static boolean jumpToDetailSetting(Context context, String packageName) {
        if (context == null)
            return false;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        }
        return false;
    }


    public static void install(Context context, File destFile) {
        try {
            if (destFile == null || !destFile.exists()) {
                AppDebugLog.d(AppDebugLog.TAG_UTIL, "安装文件不存在!" + (destFile == null ? "null" : destFile
                        .getAbsolutePath()));
                return;
            }
            String destFilePath = destFile.getAbsolutePath();
            if (checkIfDataDir(destFilePath)) {
                FileUtil.chmod(destFile.getParentFile(), "701");
                FileUtil.chmod(destFile, "604");
            }

            InstallApkByFilePath(context, destFilePath);
        } catch (Throwable e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
    }

    public static boolean checkIfDataDir(String dirPath) {
        boolean res = false;
        if (dirPath.startsWith("/data/")) {
            res = true;
        }
        return res;
    }

    public static boolean isPkgInstalled(Context context, String pkgName) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        return packageInfo != null;
    }

    public static Intent getInstallApkIntentByApkFilePath(Context context, String filePath) {
        try {
            if (filePath == null) {
                return null;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        } catch (Throwable e) {
        }
        return null;
    }

    public static void InstallApkByFilePath(Context context, String filePath) {
        if (filePath == null) {
            return;
        }
        try {
            Intent intent = getInstallApkIntentByApkFilePath(context, filePath);
            if (intent != null) {
                context.startActivity(intent);
            }
        } catch (Throwable e) {
        }

    }

    public static void uninstall(Context context, String pkgName) {
        try {
            Uri packageURI = Uri.parse("package:" + pkgName);
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
            uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(uninstallIntent);
        } catch (Throwable e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
    }

    public static void jumpToApp(Context context, JsonAppInfo info) {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "执行跳转APP任务");
        if (context == null || info == null)
            return;
        if (TextUtils.isEmpty(info.uri)) {
            jumpToApp(context, info.pkg);
            return;
        }
        Intent intent;
        try {
            intent = Intent.parseUri(info.uri, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            intent = new Intent();
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (info.start) {
            case 0:
            default:
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "通过打开Activity跳转");
                context.startActivity(intent);
                break;
            case 1:
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "通过打开Service跳转");
                context.startService(intent);
                break;
            case 2:
                AppDebugLog.d(AppDebugLog.TAG_STALKER, "通过发送Broadcast跳转");
                context.sendBroadcast(intent);
                break;
        }
    }

    private static void jumpToApp(Context context, String pkg) {
        AppDebugLog.d(AppDebugLog.TAG_STALKER, "默认跳转APP方式");
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void jumpToHome(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean isPkgForeground(Context context, String pkg) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // 21 以前使用判断应用是否处于前台
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
            return !TextUtils.isEmpty(pkg) && pkg.equals(cn.getPackageName());
        } else {
            // 21 以后通过/proc判断
            List<AndroidAppProcess> processes = AndroidProcesses.getRunningForegroundApps(context);
            if (processes != null) {
                for (AndroidAppProcess process : processes) {
                    if (process != null && process.getPackageName() != null
                            && process.getPackageName().equalsIgnoreCase(pkg)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasAllPermissions(Context context) {
        return context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && context.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isAccessibleEnabled(Context context) {
        List<AccessibilityServiceInfo> infos = ((AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE))
                .getEnabledAccessibilityServiceList(-1);
        final String service = context.getPackageName() + "/" + BuildConfig.APPLICATION_ID + ".service.NBAccessibilityService";
        for (AccessibilityServiceInfo info : infos) {
            if (info.getId().equals(service)) {
                return true;
            }
        }
        return false;
    }
}
