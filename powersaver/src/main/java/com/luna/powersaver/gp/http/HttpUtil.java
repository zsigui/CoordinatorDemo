package com.luna.powersaver.gp.http;

import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.CommonUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * Created by zsigui on 17-1-17.
 */

public class HttpUtil {

    private static void addSSLFactory(HttpURLConnection connection, boolean isHttps) {
        if (isHttps) {
            try {
                // 设置默认自定义Https的认证方式
                TrustManager[] tm = {new SGDefaultX509TrustManager()};
                SSLContext sslContext = SSLContext.getInstance("TSL");
                sslContext.init(null, tm, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(null);
                HttpsURLConnection.setDefaultHostnameVerifier(new SGDefaultHostnameVerifier());
                // 从上述SSLContext对象中得到SSLSocketFactory对象
                ((HttpsURLConnection)connection).setSSLSocketFactory(sslContext.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private static void addHeader(HashMap<String, String> headers, HttpURLConnection connection) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 处理连接后的读入数据
     */
    private static <T> void handleConnectInput(RequestListener<T> listener, HttpURLConnection connection) throws IOException {
        int code = connection.getResponseCode();
        String msg = connection.getResponseMessage();
        if (isSuccess(code)) {
            InputStream is = connection.getInputStream();
            if (isGzipStream(connection)) {
                is = new GZIPInputStream(is);
            }
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bs = new byte[4096];
            int len;
            while ((len = bis.read(bs)) != -1) {
                baos.write(bs, 0, len);
            }
            bis.close();
            is.close();
            baos.close();
            if (listener != null) {
                listener.onFinished(code, msg, listener.convertBytes(baos.toByteArray()));
            }
        } else {
            if (listener != null) {
                listener.onFinished(code, msg, null);
            }
        }
    }

    /**
     * 判断是否是gzip压缩流
     */
    public static boolean isGzipStream(final HttpURLConnection urlConnection) {
        String encoding = urlConnection.getContentEncoding();
        return encoding != null && encoding.contains("gzip");
    }

    public static boolean isSuccess(int httpStatus) {
        return httpStatus >= 200 && httpStatus < 400;
    }

    public static String convertParamMapToString(HashMap<String, String> params,  String encodeCharset) {
        if (CommonUtil.isEmpty(params)) {
            AppDebugLog.d(AppDebugLog.TAG_DOWNLOAD, "convertParamMapToString : you need to call method putParam or setParams first");
            return "";
        }
        final StringBuilder result = new StringBuilder();
        try {
            if (params != null && params.size() > 0) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    result.append(URLEncoder.encode(param.getKey(), encodeCharset));
                    result.append("=");
                    result.append(URLEncoder.encode(param.getValue(), encodeCharset));
                    result.append("&");
                }
                result.deleteCharAt(result.length() - 1);
            }
        } catch (Exception e) {
            if (AppDebugLog.IS_DEBUG) {
                AppDebugLog.d(AppDebugLog.TAG_UTIL, String.format("convertParamMapToString : the charset %s is not supported", encodeCharset));
                e.printStackTrace();
            }
        }
        return result.toString();
    }


    public static <T> void doGet(String requestUrl, HashMap<String, String> params,
                                 HashMap<String, String> headers, RequestListener<T> listener) {
        HttpURLConnection connection = null;
        try {
            if (params != null) {
                // 拼接GET的请求参数
                String param = convertParamMapToString(params, CommonUtil.DEFAULT_SYS_CHARSET);
                if (requestUrl.lastIndexOf('?') == -1) {
                    requestUrl += ('?' + param);
                } else if (requestUrl.charAt(requestUrl.length() - 1) != '?') {
                    requestUrl += param;
                } else {
                    requestUrl += ('&' + param);
                }
            }
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            addSSLFactory(connection, "https".equalsIgnoreCase(url.getProtocol()));
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setFixedLengthStreamingMode(0);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setConnectTimeout(DownloadManager.NET_CONNECT_TIMEOUT);
            connection.setReadTimeout(DownloadManager.NET_READ_TIMEOUT);
            addHeader(headers, connection);
//            connection.connect();
            handleConnectInput(listener, connection);
        } catch (Throwable e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onFailed(e);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        }
    }

    public static <T> void doPost(String requestUrl, HashMap<String, String> params,
                                  HashMap<String, String> headers, RequestListener<T> listener) {
        String param = "";
        if (params != null) {
            param = convertParamMapToString(params, CommonUtil.DEFAULT_SYS_CHARSET);
        }
        try {
            doPost(requestUrl, param.getBytes(CommonUtil.DEFAULT_SYS_CHARSET), headers, listener);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static <T> void doPost(String requestUrl, byte[] param,
                                  HashMap<String, String> headers, RequestListener<T> listener) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            addSSLFactory(connection, "https".equalsIgnoreCase(url.getProtocol()));
            connection.setDoInput(true);
            connection.setDoOutput(true);
            if (param != null){
                connection.setFixedLengthStreamingMode(param.length);
            }
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setConnectTimeout(DownloadManager.NET_CONNECT_TIMEOUT);
            connection.setReadTimeout(DownloadManager.NET_READ_TIMEOUT);
            addHeader(headers, connection);

//            connection.connect();
            // getResponseCode() 会调用 connect() 方法
            if (param != null) {
                // 写出数据
                OutputStream os = connection.getOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(os);
                bos.write(param);
                bos.flush();
                bos.close();
                os.close();
            }
            handleConnectInput(listener, connection);
        } catch (Throwable e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onFailed(e);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

//    public static <T> void doPost(String requestUrl, HashMap<String, String> params,
//                                  HashMap<String, String> headers, RequestListener<T> listener) {
//        try {
//            String param = "";
//            if (params != null) {
//                param = convertParamMapToString(params, CommonUtil.DEFAULT_SYS_CHARSET);
//            }
//            URL url = new URL(requestUrl);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            addSSLFactory(connection, "https".equalsIgnoreCase(url.getProtocol()));
//            connection.connect();
//            connection.setDoInput(true);
//            connection.setDoOutput(true);
//            connection.setFixedLengthStreamingMode(param.length());
//            connection.setRequestMethod("POST");
//            connection.setUseCaches(false);
//            connection.setConnectTimeout(DownloadManager.NET_CONNECT_TIMEOUT);
//            connection.setReadTimeout(DownloadManager.NET_READ_TIMEOUT);
//            addHeader(headers, connection);
//
//            // 写出数据
//            OutputStream os = connection.getOutputStream();
//            BufferedOutputStream bos = new BufferedOutputStream(os);
//            bos.write(param.getBytes(CommonUtil.DEFAULT_SYS_CHARSET));
//            bos.flush();
//            bos.close();
//            os.close();
//
////            connection.connect();
//            // getResponseCode() 会调用 connect() 方法
//            handleConnectInput(listener, connection);
//        } catch (Throwable e) {
//            e.printStackTrace();
//            if (listener != null) {
//                listener.onFailed(e);
//            }
//        }
//    }

    public interface RequestListener<T> {

        T convertBytes(byte[] data);

        void onFinished(int code, String httpMsg, T data);

        void onFailed(Throwable e);
    }
}
