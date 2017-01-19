package com.luna.powersaver.gp.http;

import android.util.Log;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Created by zsigui on 17-1-18.
 */

public class SGDefaultHostnameVerifier implements HostnameVerifier {

    private static final String TAG = SGDefaultHostnameVerifier.class.toString();

    @Override
    public boolean verify(String hostname, SSLSession session) {
        Log.i(TAG, "verify(String, SSLSession) : doesn't do any verify in the default config");
        return true;
    }
}
