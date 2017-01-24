package com.luna.powersaver.gp.utils;

import android.text.TextUtils;

import com.luna.powersaver.gp.common.NetConst;
import com.luna.powersaver.gp.entity.JsonAppInfo;
import com.luna.powersaver.gp.entity.JsonConfigData;
import com.luna.powersaver.gp.entity.JsonPostData;
import com.luna.powersaver.gp.entity.JsonResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zsigui on 17-1-18.
 */

public class JsonUtil {

    public static JsonResponse<JsonConfigData> convertJsonBytesToAppConfig(byte[] data) {
        JsonResponse<JsonConfigData> result = null;
        try {
            if (data == null) {
                return null;
            }
            String decompress = EncryptUtil.decrypt(data);

            AppDebugLog.d(AppDebugLog.TAG_NET, "获取到解析数据: " + decompress);

            // 构建返回对象
            JSONObject jObj = new JSONObject(decompress);

            result = new JsonResponse<>();
            result.c = jObj.optInt("c", NetConst.STATUS_ERROR);
            result.msg = jObj.optString("msg", NetConst.STR_ERROR);
            jObj = jObj.optJSONObject("d");
            if (result.c == NetConst.STATUS_OK && jObj != null) {
                result.d = new JsonConfigData();
                result.d.frequency = jObj.optInt("frequency");
                JSONArray jArr = jObj.optJSONArray("newpkgs");
                if (jArr != null) {
                    ArrayList<JsonAppInfo> list = new ArrayList<>();
                    JsonAppInfo info;
                    JSONObject tmp;
                    for (int i = 0; i < jArr.length(); i++) {
                        tmp = jArr.optJSONObject(i);
                        if (tmp != null) {
                            info = convertJObjToJsonAppInfo(tmp);
                            list.add(info);
                        }
                    }
                    result.d.newpkgs = list;
                }
                result.d.oldpkgs = jObj.optString("oldpkgs", null);
            }
        } catch (Throwable e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String convertJsonPostDataToJson(JsonPostData data) {
        try {
            JSONObject jObj = new JSONObject();
            jObj.put("gpvc", data.gpvc);
            jObj.put("installapps", TextUtils.isEmpty(data.installapps) ? "" : data.installapps);
            jObj.put("appid", data.appid);
            jObj.put("ei", data.ei);
            jObj.put("nt", String.valueOf(data.nt));
            jObj.put("brand", data.brand);
            jObj.put("device", data.device);
            jObj.put("osvc", data.osvc);
            jObj.put("osvn", data.osvn);
            return jObj.toString();
        } catch (JSONException e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static JSONObject convertJsonAppInfoToJObj(JsonAppInfo data) {
        try {
            JSONObject jObj = new JSONObject();
            jObj.put("pkg", data.pkg == null ? "" : data.pkg);
            jObj.put("task", data.task);
            jObj.put("url", data.url == null ? "" : data.url);
            jObj.put("starttime", data.starttime);
            jObj.put("endtime", data.endtime);
            jObj.put("start", data.start);
            jObj.put("uri", data.uri);
            jObj.put("action", data.action);
            jObj.put("execstate", data.execstate);
            jObj.put("openedtime", data.openedtime);
            jObj.put("exp", data.exp);
            return jObj;
        } catch (JSONException e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static JsonAppInfo convertJObjToJsonAppInfo(JSONObject jObj) {
        JsonAppInfo info = new JsonAppInfo();
        info.pkg = jObj.optString("pkg");
        info.task = jObj.optInt("task", JsonAppInfo.TASK.DOWNLOAD_BY_GP);
        info.url = jObj.optString("url");
        info.starttime = jObj.optLong("starttime");
        info.endtime = jObj.optLong("endtime");
        info.action = jObj.optInt("action", JsonAppInfo.ACTION.NOT_WORK_AFTER_OPEN);
        info.execstate = jObj.optInt("execstate");
        info.start = jObj.optInt("start", 0);
        info.uri = jObj.optString("uri");
        info.openedtime = jObj.optInt("openedtime", 0);
        info.exp = jObj.optInt("exp", JsonAppInfo.DEFAULT_OPEN_TIME);
        return info;
    }

    public static JsonAppInfo convertJsonToJsonAppInfo(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            return convertJObjToJsonAppInfo(jObj);
        } catch (JSONException e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String convertJsonAppInfoToJson(JsonAppInfo json) {
        if (json == null) {
            return "";
        }
        JSONObject jObj = convertJsonAppInfoToJObj(json);
        if (jObj != null) {
            return jObj.toString();
        }
        return "";
    }

    public static String convertJsonAppInfoMapToJson(HashMap<String, JsonAppInfo> data) {
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<String, JsonAppInfo> entry : data.entrySet()) {
            jsonArray.put(convertJsonAppInfoToJObj(entry.getValue()));
        }
        return jsonArray.toString();
    }

    public static HashMap<String, JsonAppInfo> convertJsonStringToJsonAppInfoMap(String json) {
        if (TextUtils.isEmpty(json))
            return null;
        HashMap<String, JsonAppInfo> result = null;
        try {
            JSONArray jsonArray = new JSONArray(json);
            JsonAppInfo info;
            JSONObject jObj;
            result = new HashMap<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                jObj = jsonArray.optJSONObject(i);
                if (jObj != null) {
                    info = convertJObjToJsonAppInfo(jObj);
                    result.put(info.pkg, info);
                }
            }
        } catch (JSONException e) {
            if (AppDebugLog.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
