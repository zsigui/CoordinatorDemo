package com.luna.powersaver.gp.manager;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.luna.powersaver.gp.BatteryChangeReceiver;
import com.luna.powersaver.gp.StaticConst;

/**
 * Created by zsigui on 17-1-12.
 */

public class BatteryTimeManager {

    // 指定充电的三种阶段，快速充电，持续充电，涓流充电
    public static final int STATE_QUICK = 0;
    public static final int STATE_CONTINUOUS = 1;
    public static final int STATE_TRICKLE = 2;

    public static final int LIMIT_QUICK_CHARGE = 90;
    public static final int LIMIT_CONTINUOUS_CHARGE = 100;

    private static BatteryTimeManager sInstance;

    public static BatteryTimeManager get() {
        if (sInstance == null) {
            sInstance = new BatteryTimeManager();
        }
        return sInstance;
    }

    private BatteryTimeManager() {
        Log.d("ps-test", "currentPercent = " + BatteryChangeReceiver.sCurrentPercent);
        if (BatteryChangeReceiver.sCurrentPercent == -1
                || BatteryChangeReceiver.sCurrentPlug == -1) {
            initCurrentPercentAndPlug();
        }
    }

    public long calculateChargeTime() {
        long chargeSpeed = SPManager.get(StaticConst.sContext).getRealChargeSpeed();
        if (chargeSpeed == 0) {
            chargeSpeed = calculateChargeSpeedByPercentAndPlug(
                    BatteryChangeReceiver.sCurrentPercent,
                    BatteryChangeReceiver.sCurrentPlug
            );
        }
        if (BatteryChangeReceiver.sCurrentPercent < LIMIT_QUICK_CHARGE) {
            return chargeSpeed * ((long) (LIMIT_QUICK_CHARGE - BatteryChangeReceiver.sCurrentPercent))
                    + calculateChargeSpeedByPlug(BatteryChangeReceiver.sCurrentPlug) * 10;
        }
        return BatteryChangeReceiver.sCurrentPercent <= LIMIT_CONTINUOUS_CHARGE ?
                chargeSpeed * ((long) (LIMIT_CONTINUOUS_CHARGE - BatteryChangeReceiver.sCurrentPercent)) : 0;
    }

    public void writeBatteryQuickChargeSpeed(long speed, int percent, int plug) {
        if (percent >= 0 && percent < 99) {
            long limitedSpeed = limitBatteryChargeSpeed(speed, percent);
            if (percent % 3 == 0 || percent >= 97) {
                SPManager.get(StaticConst.sContext).putRealChargeSpeed(limitedSpeed);
            }
            if (limitedSpeed != 0) {
                long[] speeds = getSpeedListInDiffPercentByPlug(plug);
                int i3 = percent / 3;
                if (speeds[i3] == 0) {
                    speeds[i3] = limitedSpeed;
                } else {
                    speeds[i3] = (limitedSpeed + speeds[i3]) / 2;
                }
                concatChargeSpeedAndWriteToSP(speeds, plug);
            }
        }
    }

    private void concatChargeSpeedAndWriteToSP(long[] speeds, int type) {
        StringBuilder str = new StringBuilder("");
        for (long speed : speeds) {
            str.append(speed).append(";");
        }
        str.substring(0, str.length() - 1);
        SPManager.get(StaticConst.sContext).putChargeSpeedByPlug(str.toString(), type);
    }

    private long calculateChargeSpeedByPercentAndPlug(int percent, int plug) {
        long speed = getSpeedListInDiffPercentByPlug(plug)[percent / 3];
        if (speed == 0) {
            return getDefaultSpeedInFirst(percent, plug);
        }
        return speed;
    }

    private long[] getSpeedListInDiffPercentByPlug(int plug) {
        String speedsInStr = SPManager.get(StaticConst.sContext).getChargeSpeedByPlug(plug);
        long[] speedArr = new long[34];
        if (speedsInStr != null) {
            String[] split = speedsInStr.split(";");
            for (int i2 = 0; i2 < split.length; i2++) {
                speedArr[i2] = Long.parseLong(split[i2]);
            }
        }
        return speedArr;
    }

    private long limitBatteryChargeSpeed(long speed, int percent) {
        if (speed <= 30000) {
            return 30000;
        }
        if (percent < LIMIT_QUICK_CHARGE) {
            if (speed > 360000) {
                return 360000;
            }
            return speed;
        } else if (speed > 600000) {
            return 600000;
        } else {
            return speed;
        }
    }

    private long calculateChargeSpeedByPlug(int plug) {
        long[] percentList = getSpeedListInDiffPercentByPlug(plug);
        int count = 0;
        long result = 0;
        for (int i = 30; i <= 33; i++) {
            if (percentList[i] != 0) {
                result += percentList[i];
                count++;
            }
        }
        if (result == 0) {
            return getDefaultSpeedInFirst(100, plug);
        } else {
            return result / ((long) count);
        }
    }

    private long getDefaultSpeedInFirst(int percent, int plug) {
        switch (plug) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                if (percent < LIMIT_QUICK_CHARGE) {
                    return 148000;
                }
                return 240000;
            case BatteryManager.BATTERY_PLUGGED_USB:
                if (percent >= LIMIT_QUICK_CHARGE) {
                    return 180000;
                }
                return 80000;
            default:
                return 80000;
        }
    }

    public void writeBatteryFullChargeSpeed(long percent) {
        StringBuilder recordBuilder = new StringBuilder("");
        int count;
        SPManager sp = SPManager.get(StaticConst.sContext);
        String powerRecord = sp.getPowerRecord();
        int recordIndex = sp.getPowerRecordIndex();
        if (powerRecord == null) {
            recordBuilder.append(percent);
            count = recordIndex;
        } else {
            String[] split = powerRecord.split(",");
            if (split.length < 10) {
                powerRecord += "," + percent;
                sp.putPowerRecordIndex((recordIndex + 1) % 10);
                sp.putPowerRecord(powerRecord);
                return;
            } else if (split.length < 30) {
                recordBuilder.append(powerRecord).append(",").append(percent);
                count = (recordIndex + 1) % 30;
            } else {
                split[recordIndex] = percent + "";
                recordBuilder.append(split[0]);
                for (count = 1; count < split.length; count++) {
                    recordBuilder.append(",").append(split[count]);
                }
                count = (recordIndex + 1) % 30;
            }
        }
        sp.putPowerRecordIndex(count);
        sp.putPowerRecord(recordBuilder.toString());
    }

    private Intent getBatteryIntent() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        return StaticConst.sContext.registerReceiver(null, filter);
    }

    public int getPercent() {
        if (BatteryChangeReceiver.sCurrentPercent == -1) {
            initCurrentPercentAndPlug();
        }
        return BatteryChangeReceiver.sCurrentPercent;
    }

    public int getPlug() {
        if (BatteryChangeReceiver.sCurrentPlug == -1) {
            initCurrentPercentAndPlug();
        }
        return BatteryChangeReceiver.sCurrentPlug;
    }

    private void initCurrentPercentAndPlug() {
        Intent intent = getBatteryIntent();
        if (BatteryChangeReceiver.sCurrentPercent == -1) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int percent = level * 100 / scale;
            if (percent > 100)
                percent = 100;
            BatteryChangeReceiver.sCurrentPercent = percent;
        }
        if (BatteryChangeReceiver.sCurrentPlug == -1) {
            BatteryChangeReceiver.sCurrentPlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        }
    }

    public boolean isCharging() {
        Intent batteryStatus = getBatteryIntent();
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    /**
     * 获取当前充电的阶段
     */
    public int getChargeState() {
        int percent = getPercent();
        if (percent == LIMIT_CONTINUOUS_CHARGE) {
            return STATE_TRICKLE;
        } else if (percent > LIMIT_QUICK_CHARGE) {
            // 腾讯该处定义为90，网上查询有说明80的
            return STATE_CONTINUOUS;
        } else {
            return STATE_QUICK;
        }
    }
}
