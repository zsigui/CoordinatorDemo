package com.luna.powersaver.gp.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;

/**
 * Created by zsigui on 17-1-12.
 */

public class SPManager {

    final String SP_FILENAME = "amigo_powersaver";
    final String KEY_CHARGE_SPEED_REAL = "real_charge_speed";
    final String KEY_CHARGE_SPEED_IN_USB = "usb_charge_speed";
    final String KEY_CHARGE_SPEED_IN_AC = "ac_charge_speed";
    final String KEY_CHARGE_RECORD = "battery_charge_record";
    final String KEY_POWER_RECORD = "battery_power_record";
    final String KEY_POWER_RECORD_INDEX = "battery_record_index";

    private static SPManager sInstance;

    private SPManager(Context context){
        sp = context.getSharedPreferences(SP_FILENAME, Context.MODE_PRIVATE);
        editor = sp.edit();
    }

    public static SPManager get(Context context) {
        if (sInstance == null) {
            sInstance = new SPManager(context);
        }
        return sInstance;
    }

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;


    public String getChargeSpeedByPlug(int plug) {
        switch (plug) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return sp.getString(KEY_CHARGE_SPEED_IN_AC, null);
            case BatteryManager.BATTERY_PLUGGED_USB:
                return sp.getString(KEY_CHARGE_SPEED_IN_USB, null);
            default:
                return null;
        }
    }

    public void putChargeSpeedByPlug(String msg, int plug) {
        switch (plug) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                editor.putString(KEY_CHARGE_SPEED_IN_AC, msg).commit();
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                editor.putString(KEY_CHARGE_SPEED_IN_USB, msg).commit();
                break;
            default:
        }
    }

    public long getRealChargeSpeed() {
        return sp.getLong(KEY_CHARGE_SPEED_REAL, 0);
    }

    public void putRealChargeSpeed(long speed) {
        editor.putLong(KEY_CHARGE_SPEED_REAL, speed).commit();
    }

    public String getChargeRecord() {
        return sp.getString(KEY_CHARGE_RECORD, null);
    }

    public void putChargeRecord(String record) {
        editor.putString(KEY_CHARGE_RECORD, record).commit();
    }

    public String getPowerRecord() {
        return sp.getString(KEY_POWER_RECORD, null);
    }

    public void putPowerRecord(String record) {
        editor.putString(KEY_POWER_RECORD, record).commit();
    }

    public int getPowerRecordIndex() {
        return sp.getInt(KEY_POWER_RECORD_INDEX, 0);
    }

    public void putPowerRecordIndex(int index) {
        editor.putInt(KEY_POWER_RECORD_INDEX, index).commit();
    }
}
