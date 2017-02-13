package com.luna.powersaver.gp.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * Created by zsigui on 17-1-17.
 */

public class FileUtil {

    // 定义3MB为大小文件分隔
    private static final int BIG_FILE_COUNT = 3 * 1024 * 1024;
    /**
     * 强制命令创建文件夹
     */
    public static void forceMkDir(File directory) throws IOException {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                String message =
                        "File "
                                + directory
                                + " exists and is "
                                + "not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                // Double-check that some other thread or process hasn't made
                // the directory in the background
                if (!directory.isDirectory()) {
                    String message =
                            "Unable to create directory " + directory;
                    throw new IOException(message);
                }
            }
        }
    }

    public static boolean chmod(File file, String destFilePermission) {
        try {

            if (file == null) {
                return false;
            }

            if (!file.exists()) {
                return false;
            }

            if (destFilePermission != null) {
                StringBuilder sb = new StringBuilder(100);
                sb.append("chmod ").append(destFilePermission).append(" ").append(file.getAbsolutePath());
                String cmd = sb.toString();
                Runtime.getRuntime().exec(cmd);
                return true;
            }
        } catch (Throwable e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return false;

    }

    public static boolean mkdirs(File directory) {
        try {
            forceMkDir(directory);
            return true;
        } catch (IOException e) {
        }
        return false;
    }

    private static boolean hasExternalStoragePermission(Context context) {
        int perm = context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 获取缓存文件夹，有权限的情况下优先获取外部文件夹
     */
    public static File getOwnCacheDirectory(Context context, String cacheDir) {
        File appCacheDir = null;
        if (MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && hasExternalStoragePermission(context)) {
            appCacheDir = new File(Environment.getExternalStorageDirectory(), cacheDir);
        }
        if (appCacheDir == null || (!appCacheDir.exists() && !appCacheDir.mkdirs())) {
            appCacheDir = context.getCacheDir();
        }
        return appCacheDir;
    }

    /**
     * （同步）支持删除文件或者文件夹，使用时请注意启用线程
     * <p/>
     * 使用时<strong>请注意启用线程</strong>
     *
     * @param file 要删除的文件或者目录
     * @return true or false
     */
    public final static boolean delete(final File file) {
        try {
            if (file == null) {
                return false;
            }
            if (file.exists()) {
                if (file.isFile()) {
                    return file.delete();
                } else if (file.isDirectory()) {
                    for (File f : file.listFiles()) {
                        if (!delete(f)) {
                            return false;
                        }
                    }
                    return file.delete();
                }
            } else {
                // 因为最终目的是令该文件不存在，所以如果文件一开始就不存在，那么也就意味着删除成功
                return true;
            }
        } catch (Throwable e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean copy(File from, File to) {
        FileOutputStream fos = null;
        FileInputStream fis = null;

        long startTime = System.currentTimeMillis();
        long fileLen = 0;
        String fileNameSrc = null;
        String fileNameDest = null;
        try {
            if (from == null) {
                return false;
            }

            if (!from.exists()) {
                return false;
            }

            if (to == null) {
                return false;
            }
            try {

                fileLen = from.length();
                fileNameSrc = from.getAbsolutePath();
                fileNameDest = to.getAbsolutePath();

            } catch (Throwable e) {
            }

            fis = new FileInputStream(from);
            fos = new FileOutputStream(to);

            byte[] buff = new byte[1024];
            int len = 0;

            while ((len = fis.read(buff)) > 0) {
                fos.write(buff, 0, len);
            }

            fos.flush();
            fos.close();
            fos = null;
            return true;

        } catch (Throwable e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Throwable e) {
                if (AppDebugLog.IS_DEBUG) {
                    e.printStackTrace();
                }
            }

            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Throwable e) {
                if (AppDebugLog.IS_DEBUG) {
                    e.printStackTrace();
                }
            }

            if (AppDebugLog.IS_DEBUG) {
                long nt = System.currentTimeMillis();
                long span = nt - startTime;
                AppDebugLog.d(AppDebugLog.TAG_UTIL, String.format(Locale.ENGLISH,
                        "copy file from [%s] to [%s] , length is [%d] B , cost [%d] ms", fileNameSrc, fileNameDest,
                        fileLen,
                        span));
            }
        }
        return false;
    }

    /**
     * 从文件中读取数据存放到字节数组中
     *
     * @param file
     * @return
     */
    public static byte[] readBytes(File file) {
        byte[] result = null;
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            result = IOReadBytes(fin);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(fin);
        }
        return result;
    }


    /**
     * 从输入流中读取字节数组,该操作完成后会关闭流
     */
    public static byte[] IOReadBytes(InputStream in) {
        byte[] result = null;
        BufferedInputStream bin = (in instanceof BufferedInputStream) ? (BufferedInputStream) in : new
                BufferedInputStream(in);
        byte[] tempBuf = null;
        try {
            tempBuf = new byte[1024];
            int length;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(bin.available());
            while ((length = bin.read(tempBuf)) != -1) {
                baos.write(tempBuf, 0, length);
            }
            baos.flush();
            result = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(bin, in);
        }
        return result;
    }

    /**
     * 关闭IO流
     */
    public static void closeIO(Closeable... closeables) {
        if (closeables == null || closeables.length <= 0) {
            return;
        }
        for (Closeable c : closeables) {
            if (c == null) {
                continue;
            }
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 写入和追加字节数组类型的大文本数据到文件中
     *
     * @param file
     * @param data
     * @param append
     */
    public static void writeBytesBigToFile(File file, byte[] data, boolean append) {
        FileChannel outChannel = null;
        try {
            outChannel = new RandomAccessFile(file, "rw").getChannel();
            long size = data.length + (append ? outChannel.size() : 0);
            MappedByteBuffer mapBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, size);
            int position = append ? 0 : (int) outChannel.size();
            mapBuffer.position(position);
            mapBuffer.put(data);
            mapBuffer.force();
            mapBuffer.flip();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(outChannel);
        }
    }

    /**
     * 写入字节数组数据到指定输出流中(该方法会创建新文件)
     *
     * @param out
     * @param data
     */
    public static void writeBytes(OutputStream out, byte[] data) {
        if (out == null || data == null)
            return;

        if (out instanceof FileOutputStream) {
            // 通道方式写入
            FileChannel outChannel = null;
            try {
                outChannel = ((FileOutputStream) out).getChannel();
                outChannel.write(ByteBuffer.wrap(data));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeIO(outChannel);
            }
        } else {
            // 普通IO写入
            BufferedOutputStream bout = (out instanceof BufferedOutputStream) ?
                    (BufferedOutputStream) out : new BufferedOutputStream(out);
            try {
                bout.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeIO(bout, out);
            }
        }
    }

    /**
     * 写入字节数组数据到指定文件中(该方法会创建新文件)
     *
     * @param file
     * @param data
     */
    public static void writeBytes(File file, byte[] data) {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            int size = data.length;
            if (size > BIG_FILE_COUNT && size < Integer.MAX_VALUE) {
                // 使用Mapped
                writeBytesBigToFile(file, data, false);
            } else {
                writeBytes(fout, data);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeIO(fout);
        }
    }

    public static void writeData(File storeFile, String data) {
        try {
            if (data == null)
                return;
            writeBytes(storeFile, data.getBytes(CommonUtil.DEFAULT_SYS_CHARSET));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readData(File storeFile) {
        try {
            return new String(readBytes(storeFile), CommonUtil.DEFAULT_SYS_CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean copyApkFromAssets(Context context, String fileName, String path) {
        boolean copyIsFinish = false;
        try {
            InputStream is = context.getAssets().open(fileName);
            File file = new File(path);
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            byte[] temp = new byte[1024];
            int i = 0;
            while ((i = is.read(temp)) > 0) {
                fos.write(temp, 0, i);
            }
            fos.close();
            is.close();
            copyIsFinish = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return copyIsFinish;
    }
}
