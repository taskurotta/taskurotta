package ru.taskurotta.transport.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;


public class TaskConfigContainer implements Serializable {

    private String customId;
    private long startTime;
    private String taskList;
    private String idempotenceKey;
    private RetryPolicyConfigContainer retryPolicyConfigContainer;

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private long timeout = -1;

    public TaskConfigContainer() {
    }

    public TaskConfigContainer(String customId, long startTime, String taskList, String idempotenceKey, RetryPolicyConfigContainer retryPolicyConfigContainer, long timeout) {
        this.customId = customId;
        this.startTime = startTime;
        this.taskList = taskList;
        this.idempotenceKey = idempotenceKey;
        this.retryPolicyConfigContainer = retryPolicyConfigContainer;
        this.timeout = timeout;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }

    public RetryPolicyConfigContainer getRetryPolicyConfigContainer() {
        return retryPolicyConfigContainer;
    }

    public void setRetryPolicyConfigContainer(RetryPolicyConfigContainer retryPolicyConfigContainer) {
        this.retryPolicyConfigContainer = retryPolicyConfigContainer;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getIdempotenceKey() {
        return idempotenceKey;
    }

    public void setIdempotenceKey(String idempotenceKey) {
        this.idempotenceKey = idempotenceKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskConfigContainer that = (TaskConfigContainer) o;

        if (startTime != that.startTime) return false;
        if (timeout != that.timeout) return false;
        if (idempotenceKey != null ? !idempotenceKey.equals(that.idempotenceKey) : that.idempotenceKey != null) return false;
        if (customId != null ? !customId.equals(that.customId) : that.customId != null) return false;
        if (taskList != null ? !taskList.equals(that.taskList) : that.taskList != null) return false;
        return retryPolicyConfigContainer != null ? retryPolicyConfigContainer.equals(that.retryPolicyConfigContainer) : that.retryPolicyConfigContainer == null;

    }

    @Override
    public int hashCode() {
        int result = customId != null ? customId.hashCode() : 0;
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (taskList != null ? taskList.hashCode() : 0);
        result = 31 * result + (retryPolicyConfigContainer != null ? retryPolicyConfigContainer.hashCode() : 0);
        result = 31 * result + (int) (timeout ^ (timeout >>> 32));
        result = 31 * result + (idempotenceKey != null ? idempotenceKey.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskConfigContainer{" +
                "customId='" + customId + '\'' +
                ", startTime=" + startTime +
                ", taskList='" + taskList + '\'' +
                ", retryPolicyConfigContainer=" + retryPolicyConfigContainer +
                ", timeout=" + timeout +
                ", idempotencyKey=" + idempotenceKey +
                '}';
    }
}
