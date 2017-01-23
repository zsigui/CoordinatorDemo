package com.luna.powersaver.gp.entity;

/**
 * Created by zsigui on 17-1-17.
 */

public class JsonPostData {

    // 当前谷歌商店的版本号，无安装则为-1
    public int gpvc;

    // 当前手机已经安装的app包名，形式 "pkg1|pkg2|..."
    public String installapps;

    // 应用ID
    public String appid;

    // IMEI
    public String ei;

    // 网络类型(0:未知；-1:wifi；2:2g；3:3g；4: 4g)
    public int nt;

    // 品牌
    public String brand;

    // 设备型号
    public String device;

    // 系统版本号
    public int osvc;

    // 系统版本名
    public String osvn;
}
