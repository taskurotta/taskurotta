package ru.taskurotta.server.transport;

import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 3:07 PM
 */
public class ArgContainer {

    private String className;
    private boolean isPromise;
    private UUID taskId;
    private boolean isReady;
    private String JSONValue;

    public ArgContainer(String className, boolean isPromise, UUID taskId, boolean isReady, String JSONValue) {
        this.className = className;
        this.isPromise = isPromise;
        this.taskId = taskId;
        this.isReady = isReady;
        this.JSONValue = JSONValue;
    }

    public String getClassName() {
        return className;
    }

    public boolean isPromise() {
        return isPromise;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public boolean isReady() {
        return isReady;
    }

    public String getJSONValue() {
        return JSONValue;
    }

    public void setJSONValue(String JSONValue) {
        this.JSONValue = JSONValue;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setPromise(boolean promise) {
        isPromise = promise;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    @Override
    public String toString() {
        return "ArgContainer{" +
                "className='" + className + '\'' +
                ", isPromise=" + isPromise +
                ", taskId=" + taskId +
                ", isReady=" + isReady +
                ", JSONValue='" + JSONValue + '\'' +
                '}';
    }
}
