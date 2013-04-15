package ru.taskurotta.backend.storage.model;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 6:02 PM
 */
public class ErrorContainer {

    private String className;
    private String message;
    private String stackTrace;

    public ErrorContainer() {
    }

    public ErrorContainer(Throwable throwable) {
        this.className = throwable.getClass().getName();
        this.message = throwable.getMessage();
        this.stackTrace = stackTraceToString(throwable);
    }

    public static String stackTraceToString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClassName() {
        return className;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorContainer that = (ErrorContainer) o;

        if (className != null ? !className.equals(that.className) : that.className != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (stackTrace != null ? !stackTrace.equals(that.stackTrace) : that.stackTrace != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (stackTrace != null ? stackTrace.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ErrorContainer{" +
                "className='" + className + '\'' +
                ", message='" + message + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                "} " + super.toString();
    }

}
