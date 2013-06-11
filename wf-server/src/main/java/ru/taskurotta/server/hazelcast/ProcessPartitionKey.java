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

    public ProcessPartitionKey(UUID processId) {
        this.processId = processId;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    @Override
    public Object getPartitionKey() {
        return processId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessPartitionKey that = (ProcessPartitionKey) o;

        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return processId != null ? processId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ProcessPartitionKey{" +
                "processId=" + processId +
                "} " + super.toString();
    }
}
