package com.luna.powersaver.gp.entity;

/**
 * Created by zsigui on 17-1-17.
 */

public class JsonPostData {

    // 当前谷歌商店的版本号，无安装则为-1
    public int gpvc;

    // 当前手机已经安装的app包名，形式 "pkg1|pkg2|..."
    public String installapps;
}
