package ru.taskurotta.backend.hz.checkpoint;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.nio.DataSerializable;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

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
    public void writeData(DataOutput dataOutput) throws IOException {
        dataOutput.writeLong(processId.getMostSignificantBits());
        dataOutput.writeLong(processId.getLeastSignificantBits());

        dataOutput.writeLong(taskId.getMostSignificantBits());
        dataOutput.writeLong(taskId.getLeastSignificantBits());

        dataOutput.writeUTF(actorId);
        dataOutput.writeUTF(timeoutType.toString());
        dataOutput.writeLong(time);
    }

    @Override
    public void readData(DataInput dataInput) throws IOException {
        this.processId = new UUID(dataInput.readLong(), dataInput.readLong());
        this.taskId = new UUID(dataInput.readLong(), dataInput.readLong());

        this.actorId = dataInput.readUTF();
        this.timeoutType = TimeoutType.valueOf(dataInput.readUTF());
        this.time = dataInput.readLong();
    }
}
