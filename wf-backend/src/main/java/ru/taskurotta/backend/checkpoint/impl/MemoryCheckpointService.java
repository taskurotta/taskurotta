package ru.taskurotta.backend.checkpoint.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;


/**
 * CheckpointService implementation with memory Set<Checkpoint> storage
 */
public class MemoryCheckpointService implements CheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryCheckpointService.class);

    private Map<TimeoutType, Set<Checkpoint>> checkpointStorage = new ConcurrentHashMap<TimeoutType, Set<Checkpoint>>();

    public void addCheckpoint(Checkpoint checkpoint) {
        if(hasTimeoutType(checkpoint)) {
            Set<Checkpoint> set = getTypedSet(checkpoint.getTimeoutType(), true);
            set.add(checkpoint);
        } else {
            logger.error("Cannot add empty type Checkpoint [{}]", checkpoint);
        }
    }

    private Set<Checkpoint> getTypedSet(TimeoutType type, boolean createIfMissing) {
        Set<Checkpoint> result = null;
        if(type!=null) {
            result = checkpointStorage.get(type);
            if(createIfMissing && result==null) {
                synchronized (this) {
                    //TODO: some other threadsafe implementation?
                    result = Collections.synchronizedSet(new HashSet<Checkpoint>());
                    checkpointStorage.put(type, result);
                }
            }
        }
        return result;
    }

    @Override
    public void removeCheckpoint(Checkpoint checkpoint) {
        if(hasTimeoutType(checkpoint)) {
            Set<Checkpoint> set = getTypedSet(checkpoint.getTimeoutType(), false);
            if(set != null) {
                set.remove(checkpoint);
            } else {
                logger.debug("Skipping checkpoint[{}] removal: storage for timeout type[{}] have not been created yet", checkpoint, checkpoint.getTimeoutType());
            }
        } else {
            logger.error("Cannot remove empty type Checkpoint [{}]", checkpoint);
        }
    }

    @Override
    public List<Checkpoint> listCheckpoints(CheckpointQuery command) {
        List<Checkpoint> result = new ArrayList<Checkpoint>();
        if(command!=null && command.getTimeoutType()!=null) {
            Set<Checkpoint> set = getTypedSet(command.getTimeoutType(), false);
            if(set != null) {
                for(Checkpoint item: set) {
                    if(validAgainstCommand(item, command)) {
                        result.add(item);
                    }
                }
            }
        } else {
            logger.debug("Cannot list checkpoint with empty type query[{}]", command);
        }
        return result;
    }

    private static boolean validAgainstCommand(Checkpoint checkpoint, CheckpointQuery command) {
        boolean result = true;
        if(checkpoint!=null && command!=null) {
            if(command.getMinTime() > 0) {
                result = result && checkpoint.getTime()>command.getMinTime();
            }
            if(command.getMaxTime() > 0) {
                result = result && checkpoint.getTime()<command.getMaxTime();
            }

            if(command.getEntityType() != null) {
                result = result && command.getEntityType().equals(checkpoint.getEntityType());
            }
        }
        return result;
    }

    @Override
    public void addCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        if(checkpoints == null) {
            return;
        }
        Set<Checkpoint> set = getTypedSet(timeoutType, true);
        set.addAll(checkpoints);
    }

    @Override
    public void removeCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        if(checkpoints == null) {
            return;
        }
        Set<Checkpoint> set = getTypedSet(timeoutType, false);
        if(set != null) {
            set.removeAll(checkpoints);
        } else {
            logger.debug("Cannot remove checkpoints of type[{}]: store have not been initialized");
        }
    }


    private List<Checkpoint> getCheckpoints(UUID uuid, TimeoutType timeoutType) {
        List<Checkpoint> result = new ArrayList<Checkpoint>();
        if(timeoutType != null) {
            Set<Checkpoint> set = getTypedSet(timeoutType, false);
            if(set != null) {
                synchronized (set) {
                    for(Checkpoint item: set) {
                        if(item.getGuid().equals(uuid)) {
                            result.add(item);
                        }
                    }
                }
            }
        } else {
            logger.debug("Cannot get checkpoint with empty timeout type [{}]", timeoutType);
        }
        return result;
    }

    private static boolean hasTimeoutType(Checkpoint checkpoint) {
        return checkpoint!=null && checkpoint.getTimeoutType()!=null;
    }

    @Override
    public int removeEntityCheckpoints(UUID uuid, TimeoutType timeoutType) {
        int result = 0;
        List<Checkpoint> checkpoints = getCheckpoints(uuid, timeoutType);
        if(checkpoints!=null && !checkpoints.isEmpty()) {
            removeCheckpoints(timeoutType, checkpoints);
            result = checkpoints.size();
        }
        return result;
    }


}
