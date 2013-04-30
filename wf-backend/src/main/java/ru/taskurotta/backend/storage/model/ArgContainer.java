package ru.taskurotta.backend.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 3:07 PM
 */
public class ArgContainer implements Cloneable {
    public enum ValueType {
        PLAIN, PROMISE, ARRAY, PROMISE_ARRAY
    }

    private String className;
    private UUID taskId;
    private boolean isReady;
    private String JSONValue;
    private ValueType type;
    private int[] data;

    public ArgContainer() {
    }

    public ArgContainer(String className, ValueType type, UUID taskId, boolean isReady, String JSONValue) {
        this.type = type;
        this.className = className;
        this.taskId = taskId;
        this.isReady = isReady;
        this.JSONValue = JSONValue;
    }

    public ArgContainer(String className, boolean isPromise, UUID taskId, boolean isReady, String JSONValue) {
        this.type = ValueType.PLAIN;
        this.className = className;
        this.taskId = taskId;
        this.isReady = isReady;
        this.JSONValue = JSONValue;
    }

    public String getClassName() {
        return className;
    }

    @JsonIgnore
    public boolean isPromise() {
        return ValueType.PROMISE.equals(type);
    }

    @JsonIgnore
    public boolean isArray() {
        return ValueType.ARRAY.equals(type);
    }

    public ValueType getType() {
        return type;
    }

    public void setType(ValueType type) {
        this.type = type;
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

    public void setReady(boolean ready) {
        isReady = ready;
    }

    @Override
    public ArgContainer clone() throws CloneNotSupportedException {
        return (ArgContainer) super.clone();
    }

    @Override
    public String toString() {
        return "ArgContainer{" +
                "className='" + className + '\'' +
                ", taskId=" + taskId +
                ", isReady=" + isReady +
                ", JSONValue='" + JSONValue + '\'' +
                ", type=" + type +
                '}';
    }
}
