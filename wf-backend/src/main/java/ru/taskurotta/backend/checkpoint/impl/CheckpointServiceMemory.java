package ru.taskurotta.backend.checkpoint.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;


/**
 * CheckpointService implementation with memory Set<Checkpoint> storage
 */
public class CheckpointServiceMemory implements CheckpointService {

    private Set<Checkpoint> checkpointStorage = new CopyOnWriteArraySet<Checkpoint>();

    public void addCheckpoint(Checkpoint checkpoint) {
        checkpointStorage.add(checkpoint);
    }

    public void removeCheckpoint(Checkpoint checkpoint) {
        checkpointStorage.remove(checkpoint);
    }

    public List<Checkpoint> listCheckpoints(CheckpointQuery command) {
        List<Checkpoint> result = new ArrayList<Checkpoint>();
        for(Checkpoint item: checkpointStorage) {
            if(validAgainstCommand(item, command)) {
                result.add(item);
            }
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
    public void addCheckpoints(List<Checkpoint> checkpoints) {
        if(checkpoints!=null && !checkpoints.isEmpty()) {
            for(Checkpoint checkpoint: checkpoints) {
                addCheckpoint(checkpoint);
            }
        }
    }

    @Override
    public void removeCheckpoints(List<Checkpoint> checkpoints) {
        if(checkpoints!=null && !checkpoints.isEmpty()) {
            for(Checkpoint checkpoint: checkpoints) {
                removeCheckpoint(checkpoint);
            }
        }
    }

}
