package ru.taskurotta.backend.hz.checkpoint;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Checkpoint service implementation using hazelcast maps as storage
 * User: dimadin
 * Date: 08.07.13 10:29
 */
public class HzCheckpointService implements CheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(HzCheckpointService.class);

    private String checkpointSetPrefix = "CHECKPOINT_";

    private HazelcastInstance hzInstance;

    @Override
    public void addCheckpoint(Checkpoint checkpoint) {
        if(hasTimeoutType(checkpoint)) {
            IMap<HzCheckpoint, UUID> checkpointsOfAType = hzInstance.getMap(getName(checkpoint.getTimeoutType()));
            checkpointsOfAType.put(new HzCheckpoint(checkpoint), checkpoint.getTaskId());
        } else {
            logger.error("Cannot add empty checkpoint["+checkpoint+"]!");
        }
    }

    @Override
    public void addCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        IMap<HzCheckpoint, UUID> checkpointsOfAType = hzInstance.getMap(getName(timeoutType));
         for (Checkpoint checkpoint: checkpoints) {
             checkpointsOfAType.put(new HzCheckpoint(checkpoint), checkpoint.getTaskId());
         }
    }

    @Override
    public void removeCheckpoint(Checkpoint checkpoint) {
        if(hasTimeoutType(checkpoint)) {
            IMap<HzCheckpoint, UUID> checkpointsOfAType = hzInstance.getMap(getName(checkpoint.getTimeoutType()));
            int initialSize = checkpointsOfAType.size();
            checkpointsOfAType.remove(new HzCheckpoint(checkpoint));
            logger.debug("Removed checkpoint [{}], remaining map size [{}], initial map size[{}]", checkpoint, hzInstance.getMap(getName(checkpoint.getTimeoutType())).size(), initialSize);

        } else {
            logger.error("Cannot remove empty checkpoint["+checkpoint+"]!");
        }
    }

    @Override
    public void removeCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        IMap<HzCheckpoint, UUID> checkpointsOfAType = hzInstance.getMap(getName(timeoutType));
        for (Checkpoint checkpoint: checkpoints) {
            if (hasTimeoutType(checkpoint)) {
                checkpointsOfAType.remove(new HzCheckpoint(checkpoint));
            } else {
                logger.error("Cannot remove empty checkpoint["+checkpoint+"]!");
            }
        }
    }

    @Override
    public List<Checkpoint> listCheckpoints(CheckpointQuery command) {
        List<Checkpoint> result = new ArrayList<>();
        if (command!=null && command.getTimeoutType()!=null) {
            IMap<HzCheckpoint, UUID> checkpointsOfAType = hzInstance.getMap(getName(command.getTimeoutType()));
            for (HzCheckpoint checkpoint: checkpointsOfAType.keySet()) {
                if(isValidAgainstCommand(checkpoint, command)) {
                    result.add(checkpoint);
                }
            }
        }
        return result;
    }

    private static boolean isValidAgainstCommand(Checkpoint checkpoint, CheckpointQuery command) {
        boolean result = true;
        if (checkpoint != null && command != null) {
            if (command.getMinTime() > 0) {
                result = checkpoint.getTime() > command.getMinTime();
            }
            if (command.getMaxTime() > 0) {
                result = result && checkpoint.getTime() < command.getMaxTime();
            }

            if(command.getActorId() != null) {
                result = result && command.getActorId().equals(checkpoint.getActorId());
            }

            if (command.getTaskId() != null) {
                result = result && command.getTaskId().equals(checkpoint.getTaskId());
            }

            if (command.getProcessId() != null) {
                result = result && command.getProcessId().equals(checkpoint.getProcessId());
            }
        }
        return result;
    }

    @Override
    public int removeTaskCheckpoints(UUID taskId, UUID processId, TimeoutType timeoutType) {
        IMap<HzCheckpoint, UUID> checkpointsOfAType = hzInstance.getMap(getName(timeoutType));
        int initialSize = checkpointsOfAType.size();
        int result = 0;
        for (HzCheckpoint checkpoint: checkpointsOfAType.keySet()) {
            if(taskId.equals(checkpoint.getTaskId()) && processId.equals(checkpoint.getProcessId())) {
                if(checkpointsOfAType.remove(checkpoint) != null) {
                    result++;
                };
            }
        }
        logger.debug("Removed [{}] checkpoints of a [{}] type, remaining map size [{}], initialSize [{}]", result, timeoutType, hzInstance.getMap(getName(timeoutType)).size(), initialSize);
        return result;
    }

    private static boolean hasTimeoutType(Checkpoint checkpoint) {
        return checkpoint != null && checkpoint.getTimeoutType() != null;
    }

    private String getName(TimeoutType timeoutType) {
        return checkpointSetPrefix + timeoutType.toString();
    }

    public void setCheckpointSetPrefix(String checkpointSetPrefix) {
        this.checkpointSetPrefix = checkpointSetPrefix;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }
}
