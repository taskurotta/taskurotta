package ru.taskurotta.backend.hz;

import com.hazelcast.core.PartitionAware;
import com.mongodb.BasicDBObject;


import java.io.Serializable;
import java.util.UUID;

/**
 * Partition aware map key for storing tasks in hazelcast. Uses processID as key
 * User: dimadin
 * Date: 08.07.13 10:08
 */
public class TaskKey extends BasicDBObject implements PartitionAware, Serializable {
    protected UUID processId;

    protected UUID id;

    public TaskKey(){

    }

    public TaskKey(UUID processId, UUID taskId) {
        this.id = taskId;
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
        if (!id.equals(taskKey.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = processId.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}
