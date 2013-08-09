package ru.taskurotta.backend.hz;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.nio.DataSerializable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Partition aware map key for storing tasks in hazelcast. Uses processID as key
 * User: dimadin
 * Date: 08.07.13 10:08
 */
public class TaskKey extends HashMap implements DataSerializable, PartitionAware {

    public TaskKey(){
    }

    public TaskKey(UUID processId, UUID taskId) {
        put("taskId", taskId);
        put("processId", processId);
    }

    @Override
    public Object getPartitionKey() {
        return get("processId");
    }

    @Override
    public void writeData(DataOutput dataOutput) throws IOException {
        UUID processId = (UUID)get("processId");
        dataOutput.writeLong(processId.getMostSignificantBits());
        dataOutput.writeLong(processId.getLeastSignificantBits());

        UUID taskId = (UUID)get("taskId");
        dataOutput.writeLong(taskId.getMostSignificantBits());
        dataOutput.writeLong(taskId.getLeastSignificantBits());
    }

    @Override
    public void readData(DataInput dataInput) throws IOException {
        UUID processId = new UUID(dataInput.readLong(), dataInput.readLong());
        UUID taskId = new UUID(dataInput.readLong(), dataInput.readLong());
        put("taskId", taskId);
        put("processId", processId);
    }

    public UUID getProcessId() {
        return (UUID)get("processId");
    }

    public void setProcessId(UUID processId) {
        put("processId", processId);
    }

    public UUID getTaskId() {
        return (UUID)get("taskId");
    }

    public void setTaskId(UUID taskId) {
        put("taskId", taskId);
    }

}
