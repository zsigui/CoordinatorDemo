package com.luna.powersaver.gp.async;

import android.os.AsyncTask;

import com.luna.powersaver.gp.BuildConfig;
import com.luna.powersaver.gp.common.NetConst;
import com.luna.powersaver.gp.common.StaticConst;
import com.luna.powersaver.gp.entity.JsonConfigData;
import com.luna.powersaver.gp.entity.JsonPostData;
import com.luna.powersaver.gp.entity.JsonResponse;
import com.luna.powersaver.gp.http.HttpUtil;
import com.luna.powersaver.gp.manager.ClockManager;
import com.luna.powersaver.gp.manager.StalkerManager;
import com.luna.powersaver.gp.utils.AppDebugLog;
import com.luna.powersaver.gp.utils.AppInfoUtil;
import com.luna.powersaver.gp.utils.EncryptUtil;
import com.luna.powersaver.gp.utils.JsonUtil;
import com.luna.powersaver.gp.utils.chiper.MD5;

import java.util.HashMap;

/**
 * Created by zsigui on 17-1-18.
 */

public class NetAsyncTask extends AsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... params) {
        // 请求获取服务器数据
        JsonPostData json = new JsonPostData();
        json.installapps = AppInfoUtil.getAppInfo(StaticConst.sContext);
        json.gpvc = AppInfoUtil.GPVC;
        String postData = JsonUtil.convertJsonPostDataToJson(json);
        AppDebugLog.d(AppDebugLog.TAG_NET, "请求参数内容: " + postData + ", md5 = " + MD5.digestInHex(postData));
        byte[] post = EncryptUtil.encrypt(postData);
        HashMap<String, String> getMap = new HashMap<>();
        getMap.put("rt", String.valueOf(System.currentTimeMillis() / 1000));
        String requestUrl = NetConst.getHost() + "?" + EncryptUtil.concatSortedByAlpha(getMap, post);
        AppDebugLog.d(AppDebugLog.TAG_NET, "网络请求地址：" + requestUrl);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("SL-PVC", String.valueOf(BuildConfig.VERSION_CODE));
        headers.put("SL-AVC", String.valueOf(BuildConfig.PROTOCOL_VERSION_CODE));

        HttpUtil.doPost(requestUrl, post, headers,
                new HttpUtil.RequestListener<JsonResponse<JsonConfigData>>() {
            @Override
            public JsonResponse<JsonConfigData> convertBytes(byte[] data) {
                return JsonUtil.convertJsonBytesToAppConfig(data);
            }

            @Override
            public void onFinished(int code, String httpMsg, JsonResponse<JsonConfigData> data) {
                if (HttpUtil.isSuccess(code) && data != null) {
                    if (data.c == NetConst.STATUS_OK) {
                        // 删除旧的
                        StalkerManager.get().removeOldTask(data.d.oldpkgs);
                        // 新的整理
                        StalkerManager.get().addNewTask(data.d.newpkgs);
                        // 重设下次请求网络闹钟
                        ClockManager.get().startOrResetAlarm(StaticConst.sContext, data.d.frequency);
                        return;
                    }
                    AppDebugLog.w(AppDebugLog.TAG_NET, "获取的服务器返回结果: code = " + data.c + ", msg = " + data.d);
                    return;
                }
                AppDebugLog.w(AppDebugLog.TAG_NET, "HTTP网络请求结果：code = " + code + ", msg = " + httpMsg);
            }

            @Override
            public void onFailed(Throwable e) {
                if (AppDebugLog.IS_DEBUG) {
                    AppDebugLog.w(AppDebugLog.TAG_NET, e);
                    e.printStackTrace();
                }
            }
        });
        return null;
    }
}
