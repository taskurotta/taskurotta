package ru.taskurotta.backend.hz.checkpoint;

import java.io.IOException;
import java.util.UUID;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;

/**
 * Hazelcast version of a Checkpoint entity: providing processId partition possibility
 * User: dimadin
 * Date: 08.07.13 11:05
 */
public class HzCheckpoint extends Checkpoint implements DataSerializable, PartitionAware {

    @Override
    public Object getPartitionKey() {
        return processId;
    }

    public HzCheckpoint() {
    }

    public HzCheckpoint(Checkpoint checkpoint) {
        this.taskId = checkpoint.getTaskId();
        this.processId = checkpoint.getProcessId();
        this.actorId = checkpoint.getActorId();
        this.timeoutType = checkpoint.getTimeoutType();
        this.time = checkpoint.getTime();
    }

    @Override
    public void writeData(ObjectDataOutput dataOutput) throws IOException {
        dataOutput.writeLong(processId.getMostSignificantBits());
        dataOutput.writeLong(processId.getLeastSignificantBits());

        dataOutput.writeLong(taskId.getMostSignificantBits());
        dataOutput.writeLong(taskId.getLeastSignificantBits());

        dataOutput.writeUTF(actorId);
        dataOutput.writeUTF(timeoutType.toString());
        dataOutput.writeLong(time);
    }

    @Override
    public void readData(ObjectDataInput dataInput) throws IOException {
        this.processId = new UUID(dataInput.readLong(), dataInput.readLong());
        this.taskId = new UUID(dataInput.readLong(), dataInput.readLong());

        this.actorId = dataInput.readUTF();
        this.timeoutType = TimeoutType.valueOf(dataInput.readUTF());
        this.time = dataInput.readLong();
    }
}
