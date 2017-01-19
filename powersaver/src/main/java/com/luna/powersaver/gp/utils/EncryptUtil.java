package com.luna.powersaver.gp.utils;

import android.util.Base64;

import com.luna.powersaver.gp.BuildConfig;
import com.luna.powersaver.gp.utils.chiper.MD5;
import com.luna.powersaver.gp.utils.chiper.XXTEA;
import com.luna.powersaver.gp.utils.chiper.Zlib;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 进行网络传输过程中加密的集合工具
 * Created by zsigui on 17-1-18.
 */
public class EncryptUtil {

    /**
     * (1) 返回参数拼接后的键值对，格式：key=value&key=value...key=value&sign=(value进行了utf-8-encode)<br />
     * (2) 签名Sign，将参数键值按字母升序排列，排序后格式：keyvaluekeyvalue...keyvalue(value无须加密) <br />
     */
    public static String concatSortedByAlpha(HashMap<String, String> paramMap, byte[] post) {
        try {
            String[] keys = new String[paramMap.size()];
            int i = 0;
            for (String key : paramMap.keySet()) {
                keys[i++] = key;
            }
            Arrays.sort(keys);
            StringBuilder signBuilder = new StringBuilder();
            StringBuilder paramBuilder = new StringBuilder();
            String value;
            for (String key : keys) {
                value = paramMap.get(key);
                signBuilder.append(key).append(value);
                paramBuilder.append(key)
                        .append('=')
                        .append(URLEncoder.encode(value, CommonUtil.DEFAULT_SYS_CHARSET))
                        .append('&');

            }
            String sign = MD5.digestInHex(signBuilder.append(MD5.digestInHex(post)).toString());
            paramBuilder.append("sign=")
                    .append(URLEncoder.encode(sign, CommonUtil.DEFAULT_SYS_CHARSET));
            return paramBuilder.toString();
        } catch (Throwable e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public static byte[] encrypt(String json) {
        byte[] result = null;
        try {

            byte[] bs = Zlib.compress(json.getBytes(CommonUtil.DEFAULT_SYS_CHARSET));
            result = XXTEA.encrypt(bs, BuildConfig.XXTEA_KEY);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String decrypt(byte[] data) {
        String result = "";
        try {
            byte[] tmp = XXTEA.decrypt(Base64.decode(data, Base64.DEFAULT), BuildConfig.XXTEA_KEY);
            result = new String(Zlib.decompress(tmp),
                    CommonUtil.DEFAULT_SYS_CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }
}
