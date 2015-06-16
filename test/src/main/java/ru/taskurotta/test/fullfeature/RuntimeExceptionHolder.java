package ru.taskurotta.test.fullfeature;

/**
 */
public class RuntimeExceptionHolder {

    public static final ThreadLocal<RuntimeException> exceptionToThrow = new ThreadLocal<>();

    public static void beOrNotToBe() {
        RuntimeException exception = exceptionToThrow.get();
        if (exception != null) {

            throw exception;
        }
    }

}
