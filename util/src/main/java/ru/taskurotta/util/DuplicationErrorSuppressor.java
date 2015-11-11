package ru.taskurotta.util;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 */
public class DuplicationErrorSuppressor {

    // todo: should be parametrized
    private long suppressDuplicationErrorMls = TimeUnit.SECONDS.toMillis(30);

    private static class LogErrorEvent {
        String msg;
        Throwable ex;
        long timeMls;

        public LogErrorEvent(String msg, Throwable ex) {
            this.msg = msg;
            this.ex = ex;
            this.timeMls = System.currentTimeMillis();
        }
    }

    private volatile LogErrorEvent lastErrorEvent;


    public boolean isLastErrorEqualsTo(String msg, Throwable ex) {

        LogErrorEvent newLogErrorEvent = new LogErrorEvent(msg, ex);

        // try to find same error
        if (lastErrorEvent != null && isRepeatedError(lastErrorEvent, newLogErrorEvent)) {

            // skip it
            return true;
        }

        lastErrorEvent = newLogErrorEvent;

        return false;
    }

    private boolean isRepeatedError(LogErrorEvent oldErrorEvent, LogErrorEvent newErrorEvent) {

        if (oldErrorEvent == null) {
            return false;
        }

        if (newErrorEvent.timeMls - suppressDuplicationErrorMls > oldErrorEvent.timeMls) {
            return false;
        }

        if (!newErrorEvent.msg.equals(oldErrorEvent.msg)) {
            return false;
        }

        // we cannot use ex.equals()

        return recursionEquals(oldErrorEvent.ex, newErrorEvent.ex);
    }

    private static boolean recursionEquals(Throwable oldEx, Throwable newEx) {

        if (oldEx == null && newEx == null) {
            return true;
        }

        if (oldEx == null || newEx == null) {
            return false;
        }

        String oldExMsg = oldEx.getMessage();
        String newExMsg = newEx.getMessage();


        if (!((newExMsg == null && oldExMsg == null) || (newExMsg != null && oldExMsg != null && newExMsg
                .equals(oldExMsg)))) {
            return false;
        }

        StackTraceElement[] oldStElements = oldEx.getStackTrace();
        StackTraceElement[] newStElements = newEx.getStackTrace();

        if (!Arrays.equals(oldStElements, newStElements)) {
            return false;
        }

        return recursionEquals(oldEx.getCause(), newEx.getCause());
    }

}
