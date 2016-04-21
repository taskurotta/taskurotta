package ru.taskurotta.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 */
public class DuplicationErrorSuppressor {

    private static final Logger logger = LoggerFactory.getLogger(DuplicationErrorSuppressor.class);

    private long suppressDuplicationErrorMls;
    private boolean checkExMessages = true;

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

    private volatile LogErrorEvent lastErrorEvent = null;


    public DuplicationErrorSuppressor(long suppressDuplicationErrorMls, boolean checkExMessages) {
        this.suppressDuplicationErrorMls = suppressDuplicationErrorMls;
        this.checkExMessages = checkExMessages;
    }

    public synchronized boolean isLastErrorEqualsTo(String msg, Throwable ex) {

        LogErrorEvent newLogErrorEvent = new LogErrorEvent(msg, ex);

        // try to find same error
        if (lastErrorEvent != null && isRepeatedError(lastErrorEvent, newLogErrorEvent)) {

            logger.error("isLastErrorEqualsTo return true");
            // skip it
            return true;
        }

        lastErrorEvent = newLogErrorEvent;

        logger.error("isLastErrorEqualsTo return false");
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

    private boolean recursionEquals(Throwable oldEx, Throwable newEx) {

        if (oldEx == null && newEx == null) {
            return true;
        }

        if (oldEx == null || newEx == null) {
            return false;
        }

        if (checkExMessages) {
            String oldExMsg = oldEx.getMessage();
            String newExMsg = newEx.getMessage();


            if (!((newExMsg == null && oldExMsg == null) || (newExMsg != null && oldExMsg != null && newExMsg
                    .equals(oldExMsg)))) {
                return false;
            }
        }

        StackTraceElement[] oldStElements = oldEx.getStackTrace();
        StackTraceElement[] newStElements = newEx.getStackTrace();

        if (!Arrays.equals(oldStElements, newStElements)) {
            return false;
        }

        return recursionEquals(oldEx.getCause(), newEx.getCause());
    }

}
