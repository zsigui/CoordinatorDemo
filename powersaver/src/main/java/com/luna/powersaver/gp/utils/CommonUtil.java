package com.luna.powersaver.gp.utils;

import android.os.Handler;
import android.os.Looper;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by zsigui on 17-1-17.
 */

public final class CommonUtil {

    private static final String PRE_TAG = CommonUtil.class.getName();
    public static final String DEFAULT_SYS_CHARSET = "UTF-8";
    private static char SPECIAL_CHAR = '\0';
    private static Handler sMainHandler;

    public static Handler getAndroidHandler() {
        if (sMainHandler == null) {
            sMainHandler = new Handler(Looper.getMainLooper());
        }
        return sMainHandler;
    }

    /**
     * 判断给定对象是否为空
     *
     * @param obj
     * @return
     */
    public static boolean isEmpty(Object obj) {
        boolean result = obj == null;
        if (!result) {
            if (obj instanceof CharSequence) {
                result = ((String) obj).isEmpty();
            } else if (obj instanceof Map) {
                result = ((Map) obj).isEmpty();
            } else if (obj instanceof List) {
                result = ((List) obj).isEmpty();
            } else if (obj instanceof Set) {
                result = ((Set) obj).isEmpty();
            }
        }
        return result;
    }

    /**
     * 将字节数组转换成十六进制字符串
     *
     * @param bs
     * @return
     */
    public static String bytesToHex(byte... bs) {
        if (isEmpty(bs)) {
            throw new IllegalArgumentException(PRE_TAG + ".bytesToHex : param bs(byte...) is null");
        }
        StringBuilder builder = new StringBuilder();
        for (byte b : bs) {
            int bt = b & 0xff;
            if (bt < 16) {
                builder.append(0);
            }
            builder.append(Integer.toHexString(bt));
        }
        return builder.toString();
    }

    /**
     * 将十六进制字符串转换成字节数组
     *
     * @param hex
     * @return
     */
    public static byte[] hexToBytes(String hex) {
        if (isEmpty(hex)) {
            throw new IllegalArgumentException(PRE_TAG + ".hexToBytes : param hex(String...) is null");
        }
        if (hex.length() % 2 == 1)
            hex += '0';
        byte[] result = new byte[hex.length() / 2];
        char[] cs = hex.toLowerCase(Locale.CHINA).toCharArray();
        for (int i = 0; i < result.length; i++) {
            int pos = i * 2;
            result[i] = (byte) (charToByte(cs[pos]) << 4 | charToByte(cs[pos + 1]));
        }
        return result;
    }

    /**
     * 将字节数组转为十六进制表示的MAC地址
     *
     * @param bs
     * @return
     */
    public static String bytesToMacStr(byte[] bs) {
        if (isEmpty(bs)) {
            throw new IllegalArgumentException(PRE_TAG + ".bytesToMacStr : param bs(byte[]) is null");
        }
        StringBuilder sb = new StringBuilder(bs.length);
        for (byte b : bs) {
            sb.append(":");
            sb.append(bytesToHex(b));
        }
        sb.deleteCharAt(0);
        return sb.toString();
    }

    /**
     * 将整形转换为点十六进制表示法的IP地址
     *
     * @param info
     * @return
     */
    public static String intToIpStr(int info) {
        return (info & 0xFF) + "." + (info >> 8 & 0xFF) + "." + (info >> 16 & 0xFF) + "." + (info >> 24 & 0xFF);
    }

    /**
     * 将字符转换为字节数
     *
     * @param c
     * @return
     */
    public static byte charToByte(char c) {
        return (byte) "0123456789abcdefg".indexOf(c);
    }

    /**
     * 将字节数组转换为指定格式编码的字符串
     */
    public static String bytesToStr(byte[] data, String charset) {
        String result = null;
        try {
            result = new String(data, charset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 将字节数组转换为系统默认格式编码的字符串
     */
    public static String bytesToStr(byte[] data) {
        return bytesToStr(data, DEFAULT_SYS_CHARSET);
    }

    public static void copy(Object src, int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
    }


    public static byte[] longToBytes(long num) {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(
                ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(num);
        return buffer.array();
    }

    public static long bytesToLong(byte[] b, int index) {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(
                ByteOrder.LITTLE_ENDIAN);
        buffer.put(b, index, 8);
        return buffer.getLong(0);
    }


    public static long[] bytesToLongs(byte[] data) {
        int n = (data.length % 8 == 0 ? 0 : 1) + data.length / 8;
        long[] result = new long[n];

        for (int i = 0; i < n - 1; i++) {
            result[i] = CommonUtil.bytesToLong(data, i * 8);
        }

        byte[] buffer = new byte[8];
        for (int i = 0, j = (n - 1) * 8; j < data.length; i++, j++) {
            buffer[i] = data[j];
        }
        result[n - 1] = CommonUtil.bytesToLong(buffer, 0);

        return result;
    }

    public static byte[] longsToBytes(long[] data) {
        List<Byte> result = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            byte[] bs = CommonUtil.longToBytes(data[i]);
            for (int j = 0; j < 8; j++) {
                result.add(bs[j]);
            }
        }

        while (result.get(result.size() - 1) == SPECIAL_CHAR) {
            result.remove(result.size() - 1);
        }

        byte[] ret = new byte[result.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = result.get(i);
        }
        return ret;
    }

    public static String longsToHex(long[] data) {
        StringBuilder sb = new StringBuilder();
        for (long d : data) {
            sb.append(padLeft(Long.toHexString(d), 16));
        }
        return sb.toString();
    }

    public static long[] bytesToLongs(String data) {
        int len = data.length() / 16;
        long[] result = new long[len];
        for (int i = 0; i < len; i++) {
            result[i] = new BigInteger(data.substring(i * 16, i * 16 + 16), 16)
                    .longValue();
        }
        return result;
    }

    public static String padRight(String source, int length) {
        while (source.length() < length) {
            source += SPECIAL_CHAR;
        }
        return source;
    }

    public static String padLeft(String source, int length) {
        while (source.length() < length) {
            source = '0' + source;
        }
        return source;
    }

}
