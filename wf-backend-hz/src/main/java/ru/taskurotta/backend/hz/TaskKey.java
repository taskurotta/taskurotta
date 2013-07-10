package ru.taskurotta.backend.hz;

import com.hazelcast.core.PartitionAware;

import java.io.Serializable;
import java.util.UUID;

/**
 * Partition aware map key for storing tasks in hazelcast. Uses processID as key
 * User: dimadin
 * Date: 08.07.13 10:08
 */
public class TaskKey implements PartitionAware, Serializable {
    protected UUID processId;
    protected UUID taskId;

    public TaskKey(UUID processId, UUID taskId) {
        this.taskId = taskId;
        this.processId = processId;
    }

    @Override
    public Object getPartitionKey() {
        return processId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskKey)) return false;

        TaskKey taskKey = (TaskKey) o;

        if (!processId.equals(taskKey.processId)) return false;
        if (!taskId.equals(taskKey.taskId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = processId.hashCode();
        result = 31 * result + taskId.hashCode();
        return result;
    }
}
