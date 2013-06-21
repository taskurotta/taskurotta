package ru.taskurotta.backend.hz.checkpoint;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by void 20.06.13 11:49
 */
public class CheckpointServiceImpl implements CheckpointService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointServiceImpl.class);
    private static final String SET_NAME_PREFIX = "CheckpointSet_";
    private HazelcastInstance hzInstance;

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    private String getMapName(TimeoutType timeoutType) {
        return SET_NAME_PREFIX + timeoutType;
    }

    @Override
    public void addCheckpoint(Checkpoint checkpoint) {
        MultiMap<UUID, Checkpoint> map = hzInstance.getMultiMap(getMapName(checkpoint.getTimeoutType()));
        map.put(checkpoint.getEntityGuid(), checkpoint);
        log.debug("created checkpoint :: [{}]", checkpoint);
    }

    @Override
    public void addCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        if (checkpoints == null) {
            return;
        }
        MultiMap<UUID, Checkpoint> map = hzInstance.getMultiMap(getMapName(timeoutType));
        for (Checkpoint checkpoint : checkpoints) {
            map.put(checkpoint.getEntityGuid(), checkpoint);
        }
    }

    @Override
    public void removeCheckpoint(Checkpoint checkpoint) {
        MultiMap<UUID, Checkpoint> map = hzInstance.getMultiMap(getMapName(checkpoint.getTimeoutType()));
        Collection<Checkpoint> checkpoints = map.get(checkpoint.getEntityGuid());
        if (checkpoints.remove(checkpoint)) {
            log.debug("removed checkpoint [{}]", checkpoint);
        } else {
            log.warn("Checkpoint [{}] not found", checkpoint);
        }
    }

    @Override
    public void removeCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        MultiMap<UUID, Checkpoint> map = hzInstance.getMultiMap(getMapName(timeoutType));
        for (Checkpoint checkpoint : checkpoints) {
            map.remove(checkpoint.getEntityGuid(), checkpoint);
        }
    }

    @Override
    public List<Checkpoint> listCheckpoints(CheckpointQuery command) {
        List<Checkpoint> result = new ArrayList<>();
        if (command != null && command.getTimeoutType() != null) {
            MultiMap<UUID, Checkpoint> map = hzInstance.getMultiMap(getMapName(command.getTimeoutType()));
            for (Checkpoint checkpoint : map.values()) {
                if (testCheckpoint(checkpoint, command)) {
                    result.add(checkpoint);
                }
            }
        }
        return result;
    }

    private boolean testCheckpoint(Checkpoint checkpoint, CheckpointQuery query) {
        boolean result = true;
        if (query.getMinTime() > 0) {
            result = query.getMinTime() < checkpoint.getTime();
        }
        if (result && query.getMaxTime() > 0) {
            result = checkpoint.getTime() < query.getMaxTime();
        }
        return result;
    }

    @Override
    public int removeEntityCheckpoints(UUID uuid, TimeoutType timeoutType) {
        log.debug("before remove all {} checkpoints for {}", timeoutType, uuid);

        MultiMap<UUID, Checkpoint> map = hzInstance.getMultiMap(getMapName(timeoutType));
        Collection<Checkpoint> removed = map.remove(uuid);
        int result = null == removed ? 0 : removed.size();

        log.debug("removed {} checkpoints for {}", result, uuid);
        return result;
    }
}
