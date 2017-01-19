package com.luna.powersaver.gp.utils.log;


import android.content.Context;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Created by zsigui on 16-6-21.
 */
public class SGLog {

    public static final String DEFAULT_MESSAGE = "execute";
    public static final String NULL_TIPS = "Log with null object";
    public static final String PARAM = "Param";
    public static final String NULL = "null";

    public static final int V = 0x1;
    public static final int D = 0x2;
    public static final int I = 0x3;
    public static final int W = 0x4;
    public static final int E = 0x5;
    public static final int A = 0x6;

    private static boolean IS_SHOW_LOG = true;
    private static final int DEFAULT_STACKTRACE_INDEX = 5;

    public static void init(boolean isShowLog) {
        IS_SHOW_LOG = isShowLog;
    }

    public static void v() {
        printLog(DEFAULT_STACKTRACE_INDEX, V, null, DEFAULT_MESSAGE);
    }

    public static void v(Object msg) {
        printLog(DEFAULT_STACKTRACE_INDEX, V, null, msg);
    }

    public static void v(String tag, Object... objects) {
        printLog(DEFAULT_STACKTRACE_INDEX, V, tag, objects);
    }

    public static void v(int stacktraceIndex, String tag, Object... objects) {
        printLog(stacktraceIndex, V, tag, objects);
    }

    public static void d() {
        printLog(DEFAULT_STACKTRACE_INDEX, D, null, DEFAULT_MESSAGE);
    }

    public static void d(Object msg) {
        printLog(DEFAULT_STACKTRACE_INDEX, D, null, msg);
    }

    public static void d(String tag, Object... objects) {
        printLog(DEFAULT_STACKTRACE_INDEX, D, tag, objects);
    }

    public static void d(int stacktraceIndex, String tag, Object... objects) {
        printLog(stacktraceIndex, D, tag, objects);
    }

    public static void i() {
        printLog(DEFAULT_STACKTRACE_INDEX, I, null, DEFAULT_MESSAGE);
    }

    public static void i(Object msg) {
        printLog(DEFAULT_STACKTRACE_INDEX, I, null, msg);
    }

    public static void i(String tag, Object... objects) {
        printLog(DEFAULT_STACKTRACE_INDEX, I, tag, objects);
    }

    public static void i(int stacktraceIndex, String tag, Object... objects) {
        printLog(stacktraceIndex, I, tag, objects);
    }

    public static void w() {
        printLog(DEFAULT_STACKTRACE_INDEX, W, null, DEFAULT_MESSAGE);
    }

    public static void w(Object msg) {
        printLog(DEFAULT_STACKTRACE_INDEX, W, null, msg);
    }

    public static void w(String tag, Object... objects) {
        printLog(DEFAULT_STACKTRACE_INDEX, W, tag, objects);
    }

    public static void w(int stacktraceIndex, String tag, Object... objects) {
        printLog(stacktraceIndex, W, tag, objects);
    }

    public static void e() {
        printLog(DEFAULT_STACKTRACE_INDEX, E, null, DEFAULT_MESSAGE);
    }

    public static void e(Object msg) {
        printLog(DEFAULT_STACKTRACE_INDEX, E, null, msg);
    }

    public static void e(String tag, Object... objects) {
        printLog(DEFAULT_STACKTRACE_INDEX, E, tag, objects);
    }

    public static void e(int stacktraceIndex, String tag, Object... objects) {
        printLog(stacktraceIndex, E, tag, objects);
    }

    public static void a() {
        printLog(DEFAULT_STACKTRACE_INDEX, A, null, DEFAULT_MESSAGE);
    }

    public static void a(Object msg) {
        printLog(DEFAULT_STACKTRACE_INDEX, A, null, msg);
    }

    public static void a(String tag, Object... objects) {
        printLog(DEFAULT_STACKTRACE_INDEX, A, tag, objects);
    }

    public static void a(int stacktraceIndex, String tag, Object... objects) {
        printLog(stacktraceIndex, A, tag, objects);
    }

    public static void file(Context context, Object msg) {
        printFile(context, DEFAULT_STACKTRACE_INDEX, null, null, null, msg);
    }

    public static void file(Context context, String tag, String filename, Object msg) {
        printFile(context, DEFAULT_STACKTRACE_INDEX, tag, null, filename, msg);
    }

    public static void file(Context context, String tag, String directory, String filename, Object msg) {
        printFile(context, DEFAULT_STACKTRACE_INDEX, tag, directory, filename, msg);
    }

    public static void file(Context context, int stacktraceIndex, String tag, String directory, String filename, Object... msg) {
        printFile(context, stacktraceIndex, tag, directory, filename, msg);
    }

    private static void printLog(int stacktraceIndex, int type, String tagStr, Object... objects) {

        if (!IS_SHOW_LOG) {
            return;
        }

        String[] contents = wrapperLogContent(stacktraceIndex, tagStr, objects);
        String tag = contents[0];
        String msg = contents[1];
        String headString = contents[2];

        switch (type) {
            case V:
            case D:
            case I:
            case W:
            case E:
            case A:
                BaseLog.printDefault(type, tag, headString + msg);
                break;
        }
    }


    private static void printFile(Context context, int stacktraceIndex, String tagStr, String directory, String fileName, Object...
            objectMsg) {
        if (!IS_SHOW_LOG) {
            return;
        }
        String content = wrapperFileContent(stacktraceIndex, tagStr, objectMsg);
        if (TextUtils.isEmpty(directory)) {
            directory = Utils.getOwnCacheDirectory(context, "sg_log", true);
        }
        if (TextUtils.isEmpty(directory)) {
            // 获取不到有效的存储文件夹
            return;
        }
        if (TextUtils.isEmpty(fileName)) {
            fileName = formatTime(System.currentTimeMillis(), "yyyyMMdd") + ".log";
        }
        Utils.printFile(directory, fileName, content, true);
    }

    /**
     * 要使得log具有索引效果，需要保证特定格式(不包括双引号)： "(stacktrace[i].getFileName():stacktrace[i].getLineNumber())"
     */
    private static String wrapperFileContent(int stacktraceIndex, String tagStr, Object... objects) {

        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String className = stackTrace[stacktraceIndex].getFileName();
            String methodName = stackTrace[stacktraceIndex].getMethodName();
            int lineNumber = stackTrace[stacktraceIndex].getLineNumber();
            String methodNameShort = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(formatTime(System.currentTimeMillis(), "MM-dd HH:mm:ss.sss "));
            stringBuilder.append("D/").append(tagStr == null ? "DEFAULT" : tagStr).append(": ");
            stringBuilder.append("[ (").append(className).append(":")
                    .append(lineNumber).append(")#").append(methodNameShort).append(" ] ");

            String msg = (objects == null) ? NULL_TIPS : getObjectsString(objects);
            stringBuilder.append(msg).append("\n\n");
            return stringBuilder.toString();
        } catch (Throwable t) {
            t.printStackTrace();
            return "";
        }
    }

    private static String formatTime(long time, String format) {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.CHINA);
        return sdf.format(time);
    }

    private static String[] wrapperLogContent(int stacktraceIndex, String tagStr, Object... objects) {

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String className = stackTrace[stacktraceIndex].getFileName();
        String methodName = stackTrace[stacktraceIndex].getMethodName();
        int lineNumber = stackTrace[stacktraceIndex].getLineNumber();
        String methodNameShort = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ (").append(className).append(":").append(lineNumber).append(")#").append
                (methodNameShort).append(" ] ");

        String tag = (tagStr == null ? className : tagStr);
        String msg = (objects == null) ? NULL_TIPS : getObjectsString(objects);
        String headString = stringBuilder.toString();

        return new String[]{tag, msg, headString};
    }

    private static String getObjectsString(Object... objects) {

        if (objects.length > 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\n");
            for (int i = 0; i < objects.length; i++) {
                Object object = objects[i];
                if (object == null) {
                    stringBuilder.append(PARAM).append("[").append(i).append("]").append(" = ").append(NULL).append
                            ("\n");
                } else {
                    stringBuilder.append(PARAM).append("[").append(i).append("]").append(" = ").append(object
                            .toString()).append("\n");
                }
            }
            return stringBuilder.toString();
        } else {
            return objects.length == 0 || objects[0] == null ? NULL : objects[0].toString();
        }
    }

}
