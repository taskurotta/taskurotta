package ru.taskurotta.backend.storage.model;

public class StackTraceElementContainer {
    private String declaringClass;
    private String methodName;
    private String fileName;
    private int lineNumber;

    public String getDeclaringClass() {
        return declaringClass;
    }
    public void setDeclaringClass(String declaringClass) {
        this.declaringClass = declaringClass;
    }
    public String getMethodName() {
        return methodName;
    }
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public int getLineNumber() {
        return lineNumber;
    }
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return "StackTraceElementContainer [declaringClass=" + declaringClass
                + ", methodName=" + methodName + ", fileName=" + fileName
                + ", lineNumber=" + lineNumber + "]";
    }

}
