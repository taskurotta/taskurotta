package ru.taskurotta.util;

public class StringUtils {

    public static boolean isBlank(String target) {
        return target==null || target.trim().length()==0;
    }

}
