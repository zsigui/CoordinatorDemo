package com.luna.powersaver.gp.utils;

import java.util.Date;
import java.util.Locale;

/**
 * Created by zsigui on 17-1-10.
 */

public class DateUtil {

    public static String[] getCurrentTime() {
        Date date = new Date(System.currentTimeMillis());
        String[] result = new String[3];
        result[0] = String.format(Locale.ENGLISH, "%tR", date);
        result[1] = String.format(Locale.ENGLISH, "%tA", date);
        result[2] = String.format(Locale.ENGLISH, "%tB %td", date, date);
        return result;
    }

    public static String formatTime(long time) {
        int min, hour;
        StringBuilder builder = new StringBuilder();
        // 剩余 n 分钟
        time = time / 1000 / 60;
        hour = (int) (time / 60);
        min = (int) (time % 60);
        min = (time != 0 && min == 0 ? 1 : min);
        if (hour != 0) {
            builder.append(hour).append("hour ");
        }
        builder.append(min).append("min");
        return builder.toString();
    }
}
