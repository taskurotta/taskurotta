package ru.taskurotta.util;

/**
 */
public class Shutdown {

    private static volatile boolean isShutdownFlag = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                isShutdownFlag = true;
            }
        });
    }

    public static boolean isTrue() {
        return isShutdownFlag;
    }
}
