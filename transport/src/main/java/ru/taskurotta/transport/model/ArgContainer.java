package ru.taskurotta.transport.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ru.taskurotta.server.json.KeepAsJsonDeserialzier;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 3:07 PM
 */
@SuppressWarnings("UnusedDeclaration")
public class ArgContainer implements Cloneable, Serializable {
    public enum ValueType {
        PLAIN(0), ARRAY(1), COLLECTION(2);

        int value;

        ValueType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ValueType fromInt(int i) {
            if (i == 0) return ValueType.PLAIN;
            if (i == 1) return ValueType.ARRAY;
            if (i == 2) return ValueType.COLLECTION;
            return null;
        }

    }

    private String dataType;
    private UUID taskId;
    private boolean isReady;
    private String JSONValue;
    private ValueType valueType;
    private ArgContainer[] compositeValue;
    private boolean promise = false;
    private ErrorContainer errorContainer;

    public ArgContainer() {
    }

    public ArgContainer(String dataType, ValueType valueType, UUID taskId, boolean isReady, boolean promise, String JSONValue) {
        this.valueType = valueType;
        this.dataType = dataType;
        this.taskId = taskId;
        this.isReady = isReady;
        this.promise = promise;
        this.JSONValue = JSONValue;
    }

    public ArgContainer(String dataType, ValueType valueType, UUID taskId, boolean isReady, boolean promise, ArgContainer[] values) {
        this.valueType = valueType;
        this.dataType = dataType;
        this.taskId = taskId;
        this.isReady = isReady;
        this.promise = promise;
        this.compositeValue = values;
    }

    public ArgContainer(ArgContainer source) {
        this.valueType = source.valueType;
        this.dataType = source.dataType;
        this.taskId = source.taskId;
        this.isReady = source.isReady;
        this.JSONValue = source.JSONValue;
        this.compositeValue = source.compositeValue;
        this.promise = source.promise;
        this.errorContainer = source.errorContainer;
    }

    @JsonRawValue
    public String getJSONValue() {
        return JSONValue;
    }

    @JsonDeserialize(using = KeepAsJsonDeserialzier.class)
    public void setJSONValue(String JSONValue) {
        this.JSONValue = JSONValue;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isPromise() {
        return promise;
    }

    @JsonIgnore
    public boolean isArray() {
        return ValueType.ARRAY.equals(valueType);
    }

    @JsonIgnore
    public boolean isCollection() {
        return ValueType.COLLECTION.equals(valueType);
    }

    @JsonIgnore
    public boolean isPlain() {
        return ValueType.PLAIN.equals(valueType);
    }

    @JsonIgnore
    public boolean isNull() {
        return null == valueType;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public void setPromise(boolean promise) {
        this.promise = promise;
    }

//    /**
//     * Create new ArgContainer as a copy of this and change its value type
//     * @param valueType - new value type
//     * @return created object
//     */
//    public ArgContainer updateType(ValueType valueType) {
//        ArgContainer result = new ArgContainer(this);
//        result.valueType = valueType;
//        return result;
//    }

    /**
     * Create new ArgContainer as a copy of this and change its value type
     *
     * @param promise - new container type
     * @return created object
     */
    public ArgContainer updateType(boolean promise) {
        ArgContainer result = new ArgContainer(this);
        result.promise = promise;
        return result;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    /**
     * Create new ArgContainer as a copy of this and change its task ID
     *
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
     *
     * @param source - source of new value
     * @return created object
     */
    public ArgContainer updateValue(ArgContainer source) {
        ArgContainer result = new ArgContainer(this);
        result.dataType = source.dataType;
        result.JSONValue = source.JSONValue;
        result.compositeValue = source.compositeValue;
        result.isReady = source.isReady;
        result.promise = source.promise;
        result.valueType = source.valueType;
        return result;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public ArgContainer[] getCompositeValue() {
        return compositeValue;
    }

    public void setCompositeValue(ArgContainer[] compositeValue) {
        this.compositeValue = compositeValue;
    }

    public ErrorContainer getErrorContainer() {
        return errorContainer;
    }

    public void setErrorContainer(ErrorContainer errorContainer) {
        this.errorContainer = errorContainer;
    }

    public boolean containsError() {
        return null != errorContainer;
    }

    @Override
    public String toString() {
        return "ArgContainer{" +
                "dataType='" + dataType + '\'' +
                ", taskId=" + taskId +
                ", isReady=" + isReady +
                ", JSONValue='" + JSONValue + '\'' +
                ", valueType=" + valueType +
                ", compositeValue=" + Arrays.toString(compositeValue) +
                ", promise=" + promise +
                ", errorContainer=" + errorContainer +
                "} ";
    }
}
