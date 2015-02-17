package ru.taskurotta.test.fullfeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class RuntimeExceptionHolder {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeExceptionHolder.class);

    public static final ThreadLocal<RuntimeException> exceptionToThrow = new ThreadLocal<>();

    public static void beOrNotToBe() {
        RuntimeException exception = exceptionToThrow.get();
        if (exception != null) {

            throw exception;
        }
    }

}
