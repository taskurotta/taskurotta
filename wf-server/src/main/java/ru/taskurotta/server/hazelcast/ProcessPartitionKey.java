package ru.taskurotta.server.hazelcast;

import com.hazelcast.core.PartitionAware;

import java.io.Serializable;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 07.06.13
 * Time: 12:14
 */
public class ProcessPartitionKey implements Serializable, PartitionAware {

    private UUID processId;
    private String taskServerId;

    public ProcessPartitionKey(UUID processId, String taskServerId) {
        this.processId = processId;
        this.taskServerId = taskServerId;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public String getTaskServerId() {
        return taskServerId;
    }

    public void setTaskServerId(String taskServerId) {
        this.taskServerId = taskServerId;
    }

    @Override
    public Object getPartitionKey() {
        return taskServerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessPartitionKey that = (ProcessPartitionKey) o;

        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        if (taskServerId != null ? !taskServerId.equals(that.taskServerId) : that.taskServerId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = processId != null ? processId.hashCode() : 0;
        result = 31 * result + (taskServerId != null ? taskServerId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProcessPartitionKey{" +
                "processId=" + processId +
                ", taskServerId='" + taskServerId + '\'' +
                "} " + super.toString();
    }
}
