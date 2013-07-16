package ru.taskurotta.backend.hz;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.nio.DataSerializable;
import com.mongodb.BasicDBObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

/**
 * Partition aware map key for storing tasks in hazelcast. Uses processID as key
 * User: dimadin
 * Date: 08.07.13 10:08
 */
public class TaskKey extends BasicDBObject implements DataSerializable, PartitionAware  {
    protected UUID processId;
    protected UUID taskId;

    public TaskKey(){
    }

    public TaskKey(UUID processId, UUID taskId) {
        this.taskId = taskId;
        this.processId = processId;
    }

    @Override
    public Object getPartitionKey() {
        return processId;
    }

    @Override
    public void writeData(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(taskId!=null? taskId.toString(): null);
        dataOutput.writeUTF(processId!=null? processId.toString(): null);
    }

    @Override
    public void readData(DataInput dataInput) throws IOException {
        this.taskId = UUID.fromString(dataInput.readUTF());
        this.processId = UUID.fromString(dataInput.readUTF());
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }
}
