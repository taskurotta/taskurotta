package ru.taskurotta.service.hz;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Partition aware map key for storing tasks in hazelcast. Uses processID as key
 * User: dimadin
 * Date: 08.07.13 10:08
 */
public class TaskFatKey extends HashMap implements DataSerializable, PartitionAware {
    protected static final String TASK_ID = "taskId";
    protected static final String PROCESS_ID = "processId";

    public TaskFatKey() {
    }

    public TaskFatKey(UUID processId, UUID taskId) {
        put(TASK_ID, taskId);
        put(PROCESS_ID, processId);
    }

    @Override
    public Object getPartitionKey() {
        return get(PROCESS_ID);
    }

    @Override
    public void writeData(ObjectDataOutput dataOutput) throws java.io.IOException {
        UUID processId = (UUID) get(PROCESS_ID);
        dataOutput.writeLong(processId.getMostSignificantBits());
        dataOutput.writeLong(processId.getLeastSignificantBits());

        UUID taskId = (UUID) get(TASK_ID);
        dataOutput.writeLong(taskId.getMostSignificantBits());
        dataOutput.writeLong(taskId.getLeastSignificantBits());
    }

    @Override
    public void readData(ObjectDataInput dataInput) throws IOException {
        UUID processId = new UUID(dataInput.readLong(), dataInput.readLong());
        UUID taskId = new UUID(dataInput.readLong(), dataInput.readLong());
        put(TASK_ID, taskId);
        put(PROCESS_ID, processId);
    }

    public UUID getProcessId() {
        return (UUID) get(PROCESS_ID);
    }

    public void setProcessId(UUID processId) {
        put(PROCESS_ID, processId);
    }

    public UUID getTaskId() {
        return (UUID) get(TASK_ID);
    }

    public void setTaskId(UUID taskId) {
        put(TASK_ID, taskId);
    }

}
