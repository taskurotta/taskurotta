package ru.taskurotta.backend.checkpoint.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;


/**
 * CheckpointService implementation with memory Set<Checkpoint> storage
 */
public class CheckpointServiceMemory implements CheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(CheckpointServiceMemory.class);

    private Map<String, Set<Checkpoint>> checkpointStorage = new ConcurrentHashMap<String, Set<Checkpoint>>();

    public void addCheckpoint(Checkpoint checkpoint) {
        if(isTyped(checkpoint)) {
            Set<Checkpoint> set = getTypedSet(checkpoint.getType(), true);
            set.add(checkpoint);
        } else {
            logger.error("Cannot add empty type Checkpoint [{}]", checkpoint);
        }
    }


    private Set<Checkpoint> getTypedSet(String type, boolean createIfMissing) {
        Set<Checkpoint> result = null;
        if(type!=null) {
            result = checkpointStorage.get(type);
            if(createIfMissing && result==null) {
                synchronized (this) {
                    result = new HashSet<Checkpoint>();
                    checkpointStorage.put(type, result);
                }
            }
        }
        return result;
    }

    private static boolean isTyped(Checkpoint checkpoint) {
        return checkpoint!=null && checkpoint.getType()!=null && checkpoint.getType().trim().length()>0;
    }

    @Override
    public void removeCheckpoint(Checkpoint checkpoint) {
        if(isTyped(checkpoint)) {
            Set<Checkpoint> set = getTypedSet(checkpoint.getType(), false);
            if(set != null) {
                set.remove(checkpoint);
            } else {
                logger.debug("Skipping checkpoint[{}] removal: storage for type[{}] have not been created yet", checkpoint, checkpoint.getType());
            }
        } else {
            logger.error("Cannot remove empty type Checkpoint [{}]", checkpoint);
        }
    }

    @Override
    public List<Checkpoint> listCheckpoints(CheckpointQuery command) {
        List<Checkpoint> result = new ArrayList<Checkpoint>();
        if(command!=null && command.getType()!=null && command.getType().trim().length()>0) {
            Set<Checkpoint> set = getTypedSet(command.getType(), false);
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

            if(command.getType() != null) {
                result = result && command.getType().equals(command.getType());
            }
        }
        return result;
    }

    @Override
    public void addCheckpoints(String type, List<Checkpoint> checkpoints) {
        if(checkpoints == null) {
            return;
        }
        Set<Checkpoint> set = getTypedSet(type, true);
        set.addAll(checkpoints);
    }

    @Override
    public void removeCheckpoints(String type, List<Checkpoint> checkpoints) {
        if(checkpoints == null) {
            return;
        }
        Set<Checkpoint> set = getTypedSet(type, false);
        if(set != null) {
            set.removeAll(checkpoints);
        } else {
            logger.debug("Cannot remove checkpoints of type[{}]: store have not been initialized");
        }
    }


    @Override
    public List<Checkpoint> getCheckpoints(UUID uuid, String type) {
        List<Checkpoint> result = new ArrayList<Checkpoint>();
        if(type!=null && type.trim().length()>0) {
            Set<Checkpoint> set = getTypedSet(type, false);
            if(set != null) {
                for(Checkpoint item: set) {
                    if(item.getGuid().equals(uuid)) {
                        result.add(item);
                    }
                }
            }
        } else {
            logger.debug("Cannot get checkpoint with empty type [{}]", type);
        }
        return result;
    }

}
