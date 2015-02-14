package ru.taskurotta.util;

import java.util.concurrent.ExecutorService;

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

    public static void addHook(final ExecutorService executorService) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                executorService.shutdown();
            }
        });
    }
}
