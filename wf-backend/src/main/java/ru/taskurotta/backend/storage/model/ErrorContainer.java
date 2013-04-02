package ru.taskurotta.backend.storage.model;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 6:02 PM
 */
public class ErrorContainer {

    private String className;
    private String message;
    private String stackTrace;

    private boolean shouldBeRestarted;
    private long restartTime;

    public String getClassName() {
        return className;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public boolean isShouldBeRestarted() {
        return shouldBeRestarted;
    }

    public long getRestartTime() {
        return restartTime;
    }
}
