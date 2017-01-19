package com.luna.powersaver.gp.entity;

import java.util.ArrayList;

/**
 * Created by zsigui on 17-1-17.
 */

public class JsonConfigData {

    // SDK请求频率(minute)
    public int frequency;

    // 可选，需要新增下载的包名列表
    public ArrayList<JsonAppInfo> newpkgs;

    // 可选，需要删除的包名列表，形式 "pkg1|pkg2|pkg3|..."
    public String oldpkgs;
}
