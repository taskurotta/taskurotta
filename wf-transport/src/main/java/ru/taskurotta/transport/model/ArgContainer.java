package ru.taskurotta.transport.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 3:07 PM
 */
@SuppressWarnings("UnusedDeclaration")
public class ArgContainer implements Cloneable {
    public enum ValueType {
        PLAIN, PROMISE, ARRAY, OBJECT_ARRAY
    }

    private String className;
    private UUID taskId;
    private boolean isReady;
    private String JSONValue;
    private ValueType type;
    private ArgContainer[] compositeValue;

    public ArgContainer() {
    }

    public ArgContainer(String className, ValueType type, UUID taskId, boolean isReady, String JSONValue) {
        this.type = type;
        this.className = className;
        this.taskId = taskId;
        this.isReady = isReady;
        this.JSONValue = JSONValue;
    }

    public ArgContainer(String className, ValueType type, UUID taskId, boolean isReady, ArgContainer[] values) {
        this.type = type;
        this.className = className;
        this.taskId = taskId;
        this.isReady = isReady;
        this.compositeValue = values;
    }

    public ArgContainer(ArgContainer source) {
        this.type = source.type;
        this.className = source.className;
        this.taskId = source.taskId;
        this.isReady = source.isReady;
        this.JSONValue = source.JSONValue;
        this.compositeValue = source.compositeValue;
    }

    public String getClassName() {
        return className;
    }

    private void setClassName(String className) {
        this.className = className;
    }

    @JsonIgnore
    public boolean isPromise() {
        return ValueType.PROMISE.equals(type);
    }

    @JsonIgnore
    public boolean isArray() {
        return ValueType.ARRAY.equals(type);
    }

    @JsonIgnore
    public boolean isObjectArray() {
        return ValueType.OBJECT_ARRAY.equals(type);
    }

    public ValueType getType() {
        return type;
    }

    private void setType(ValueType type) {
        this.type = type;
    }

    /**
     * Create new ArgContainer as a copy of this and change its value type
     * @param type - new value type
     * @return created object
     */
    public ArgContainer updateType(ValueType type) {
        ArgContainer result = new ArgContainer(this);
        result.type = type;
        return result;
    }

    public UUID getTaskId() {
        return taskId;
    }

    private void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    /**
     * Create new ArgContainer as a copy of this and change its task ID
     * @param taskId - new task ID
     * @return created object
     */
    public ArgContainer updateTaskId(UUID taskId) {
        ArgContainer result = new ArgContainer(this);
        result.taskId = taskId;
        return result;
    }

    /**
     * Create new ArgContainer as a copy of this and change its value fields
     * @param source - source of new value
     * @return created object
     */
    public ArgContainer updateValue(ArgContainer source) {
        ArgContainer result = new ArgContainer(this);
        result.className = source.className;
        result.JSONValue = source.JSONValue;
        result.compositeValue = source.compositeValue;
        result.isReady = source.isReady;
        return result;
    }

    public boolean isReady() {
        return isReady;
    }

    private void setReady(boolean ready) {
        isReady = ready;
    }

    public String getJSONValue() {
        return JSONValue;
    }

    private void setJSONValue(String JSONValue) {
        this.JSONValue = JSONValue;
    }

    public ArgContainer[] getCompositeValue() {
        return compositeValue;
    }

    private void setCompositeValue(ArgContainer[] compositeValue) {
        this.compositeValue = compositeValue;
    }

    @Override
    public String toString() {
        return "ArgContainer{" +
                "className='" + className + '\'' +
                ", taskId=" + taskId +
                ", isReady=" + isReady +
                ", JSONValue='" + JSONValue + '\'' +
                ", type=" + type +
                ", compositeValue=" + Arrays.toString(compositeValue) +
                '}';
    }
}
