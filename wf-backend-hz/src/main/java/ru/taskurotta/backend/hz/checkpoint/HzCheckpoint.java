package ru.taskurotta.backend.hz.checkpoint;

import com.hazelcast.core.PartitionAware;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;

import java.io.Serializable;

/**
 * Hazelcast version of a Checkpoint entity: providing processId partition possibility
 * User: dimadin
 * Date: 08.07.13 11:05
 */
public class HzCheckpoint extends Checkpoint implements Serializable, PartitionAware {

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
}
