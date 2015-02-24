package ru.taskurotta.transport.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 6:02 PM
 */
public class ErrorContainer implements Serializable {

    private String[] classNames;
    private String message;
    private String stackTrace;
    private boolean fatalError = false;

    public ErrorContainer() {
    }

    public ErrorContainer(String[] classNames, String message, String stackTrace, boolean fatalError) {
        this.classNames = classNames;
        this.message = message;
        this.stackTrace = stackTrace;
        this.fatalError = fatalError;
    }

    public ErrorContainer(Throwable throwable) {
        ArrayList<String> namesList = new ArrayList<String>();
        Class parent = throwable.getClass();
        do {
            namesList.add(parent.getName());
            parent = parent.getSuperclass();
        } while (Object.class != parent);
        classNames = namesList.toArray(new String[namesList.size()]);
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

    public void setClassNames(String[] classNames) {
        this.classNames = classNames;
    }

    public String[] getClassNames() {
        return classNames;
    }

    @JsonIgnore
    public String getClassName() {
        return classNames == null || classNames.length == 0 ? "null" : classNames[0];
    }

    public void setMessage(String message) {
        this.message = message;
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

    public boolean isFatalError() {
        return fatalError;
    }

    public void setFatalError(boolean fatalError) {
        this.fatalError = fatalError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorContainer that = (ErrorContainer) o;

        if (fatalError != that.fatalError) return false;
        if (!Arrays.equals(classNames, that.classNames)) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (stackTrace != null ? !stackTrace.equals(that.stackTrace) : that.stackTrace != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = classNames != null ? Arrays.hashCode(classNames) : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (stackTrace != null ? stackTrace.hashCode() : 0);
        result = 31 * result + (fatalError ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ErrorContainer{" +
                "classNames=" + Arrays.toString(classNames) +
                ", message='" + message + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                ", fatalError=" + fatalError +
                '}';
    }
}
