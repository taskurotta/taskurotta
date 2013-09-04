package ru.taskurotta.exception.test;

/**
 * User: dimadin
 * Date: 13.05.13 12:01
 */
public class CrazyUtils {

    //Method to determine if exception is caused by craziness
    public static boolean isCrazy(Throwable ex) {
        return "ru.taskurotta.test.monkey.CrazyException".equals(ex.getClass().getName()) || (ex.getCause()!=null && isCrazy(ex.getCause()));
    }

}
