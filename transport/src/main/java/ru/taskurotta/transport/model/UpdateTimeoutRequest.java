package ru.taskurotta.transport.model;

import java.io.Serializable;
import java.util.UUID;

/**
 */
public class UpdateTimeoutRequest implements Serializable {

    UUID taskId;
    UUID processId;
    long timeout;

    public UpdateTimeoutRequest() {

    }

    public UpdateTimeoutRequest(UUID taskId, UUID processId, long timeout) {
        this.taskId = taskId;
        this.processId = processId;
        this.timeout = timeout;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getProcessId() {
        return processId;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpdateTimeoutRequest that = (UpdateTimeoutRequest) o;

        if (timeout != that.timeout) return false;
        if (!taskId.equals(that.taskId)) return false;
        return processId.equals(that.processId);

    }

    @Override
    public int hashCode() {
        int result = taskId.hashCode();
        result = 31 * result + processId.hashCode();
        result = 31 * result + (int) (timeout ^ (timeout >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "UpdateTimeoutRequest{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", timeout=" + timeout +
                '}';
    }
}
