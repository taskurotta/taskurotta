package ru.taskurotta.backend.checkpoint.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * CheckpointService implementation with memory Set<Checkpoint> storage
 */
public class MemoryCheckpointService implements CheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryCheckpointService.class);

    private Map<TimeoutType, Set<Checkpoint>> checkpointStorage = new ConcurrentHashMap<TimeoutType, Set<Checkpoint>>();

    public void addCheckpoint(Checkpoint checkpoint) {
        if (hasTimeoutType(checkpoint)) {
            Set<Checkpoint> set = getTypedSet(checkpoint.getTimeoutType(), true);
            set.add(checkpoint);
            logger.debug("created checkpoint[{}]", checkpoint);
        } else {
            logger.error("Cannot add empty type Checkpoint [{}]", checkpoint);
        }
    }

    private Set<Checkpoint> getTypedSet(TimeoutType type, boolean createIfMissing) {
        Set<Checkpoint> result = null;

        if (type != null) {
            result = checkpointStorage.get(type);
            if (createIfMissing && result == null) {
                synchronized (this) {
                    result = checkpointStorage.get(type);
                    if (result == null) {
                        result = Collections.synchronizedSet(new HashSet<Checkpoint>());
                        checkpointStorage.put(type, result);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void removeCheckpoint(Checkpoint checkpoint) {
        if (!hasTimeoutType(checkpoint)) {
            logger.error("Cannot remove empty type Checkpoint [{}]", checkpoint);
            return;
        }

        Set<Checkpoint> set = getTypedSet(checkpoint.getTimeoutType(), false);
        if (set == null) {
            logger.warn("Skipping checkpoint[{}] removal: storage for timeout type[{}] have not been created yet", checkpoint, checkpoint.getTimeoutType());
            return;
        }

        if (set.remove(checkpoint)) {
            logger.debug("removed checkpoint[{}]", checkpoint);
        } else {
            logger.warn("Checkpoint[{}] not found", checkpoint);
        }
    }

    @Override
    public List<Checkpoint> listCheckpoints(CheckpointQuery command) {
        List<Checkpoint> result = new ArrayList<>();
        if (command != null && command.getTimeoutType() != null) {
            Set<Checkpoint> set = getTypedSet(command.getTimeoutType(), false);
            if (set != null) {
                synchronized (set) {
                    for (Checkpoint item : set) {
                        if (validAgainstCommand(item, command)) {
                            result.add(item);
                        }
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
        if (checkpoint != null && command != null) {
            if (command.getMinTime() > 0) {
                result = checkpoint.getTime() > command.getMinTime();
            }
            if (command.getMaxTime() > 0) {
                result = result && checkpoint.getTime() < command.getMaxTime();
            }

            if (command.getActorId() != null) {
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
    public void addCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        if (checkpoints == null) {
            return;
        }
        Set<Checkpoint> set = getTypedSet(timeoutType, true);
        set.addAll(checkpoints);
    }

    @Override
    public void removeCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        if (checkpoints == null) {
            return;
        }
        Set<Checkpoint> set = getTypedSet(timeoutType, false);
        if (set != null) {
            set.removeAll(checkpoints);
        } else {
            logger.warn("Cannot remove checkpoints of type[{}]: store have not been initialized");
        }
    }


    private List<Checkpoint> getTaskCheckpoints(UUID taskId, TimeoutType timeoutType) {
        List<Checkpoint> result = new ArrayList<Checkpoint>();
        if (timeoutType != null) {
            Set<Checkpoint> set = getTypedSet(timeoutType, false);
            if (set != null) {
                synchronized (set) {
                    for (Checkpoint item : set) {
                        if (item.getTaskId().equals(taskId)) {
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
        return checkpoint != null && checkpoint.getTimeoutType() != null;
    }

    @Override
    public int removeTaskCheckpoints(UUID taskId, UUID processId, TimeoutType timeoutType) {
        logger.debug("before remove all {} checkpoints for taskId {}", timeoutType, taskId);
        int result = 0;
        List<Checkpoint> checkpoints = getTaskCheckpoints(taskId, timeoutType);
        if (checkpoints != null && !checkpoints.isEmpty()) {
            removeCheckpoints(timeoutType, checkpoints);
            result = checkpoints.size();
        }
        logger.debug("removed {} checkpoints for taskId {}", result, taskId);
        return result;
    }


}
