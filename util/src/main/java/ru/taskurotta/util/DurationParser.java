package ru.taskurotta.util;

import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 12.12.13
 * Time: 16:01
 */
public class DurationParser {

    public static long toMillis(String duration) {
        if (isBlank(duration)) {
            throw new RuntimeException("Duration is empty");
        }

        String timeString = duration.replaceAll("\\D", "").trim();
        String unitString = duration.replaceAll("\\d", "").trim().toUpperCase();
        if (isBlank(timeString) || isBlank(unitString)) {
            throw new RuntimeException("Duration [" + duration + "] is incorrect");
        }

        long time = Long.valueOf(timeString);
        TimeUnit timeUnit = TimeUnit.valueOf(unitString);

        return timeUnit.toMillis(time);
    }

    private static boolean isBlank(String string) {
        return string == null || string.trim().isEmpty();
    }
}
