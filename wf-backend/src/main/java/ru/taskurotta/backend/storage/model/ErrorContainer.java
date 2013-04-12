package ru.taskurotta.backend.storage.model;

import java.util.Arrays;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 6:02 PM
 */
public class ErrorContainer {

    private String className;
    private String message;
    private StackTraceElementContainer[] stackTrace;

    public ErrorContainer() {
    }

    public static StackTraceElementContainer[] convert(StackTraceElement[] stElements) {
        if (stElements == null) {
            return null;
        }
        StackTraceElementContainer[] result = new StackTraceElementContainer[stElements.length];
        for (int i = 0; i < stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            StackTraceElementContainer item = new StackTraceElementContainer();
            item.setDeclaringClass(ste.getClassName());
            item.setFileName(ste.getFileName());
            item.setLineNumber(ste.getLineNumber());
            item.setMethodName(ste.getMethodName());
            result[i] = item;
        }

        return result;
    }

    public static StackTraceElement[] convert(StackTraceElementContainer[] steContainers) {
        if (steContainers == null) {
            return null;
        }
        StackTraceElement[] result = new StackTraceElement[steContainers.length];
        for (int i = 0; i < steContainers.length; i++) {
            StackTraceElementContainer containerItem = steContainers[i];
            result[i] = new StackTraceElement(containerItem.getDeclaringClass(), containerItem.getMethodName(),
                    containerItem.getFileName(), containerItem.getLineNumber());
        }
        return result;
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

    public StackTraceElementContainer[] getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(StackTraceElementContainer[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public String toString() {
        return "ErrorContainer [className=" + className + ", message="
                + message + ", stackTrace=" + Arrays.toString(stackTrace)
                + "]";
    }

}
