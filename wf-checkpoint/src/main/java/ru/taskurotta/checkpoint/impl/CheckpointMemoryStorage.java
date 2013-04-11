package ru.taskurotta.checkpoint.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import ru.taskurotta.checkpoint.Checkpoint;
import ru.taskurotta.checkpoint.CheckpointCommand;
import ru.taskurotta.checkpoint.CheckpointService;



public class CheckpointMemoryStorage implements CheckpointService {

    private Set<Checkpoint> checkpointStorage = new CopyOnWriteArraySet<Checkpoint>();

    public void addCheckpoint(Checkpoint checkpoint) {
        checkpointStorage.add(checkpoint);
    }

    public void removeCheckpoint(Checkpoint checkpoint) {
        checkpointStorage.remove(checkpoint);
    }

    public List<Checkpoint> listCheckpoints(CheckpointCommand command) {
        List<Checkpoint> result = new ArrayList<Checkpoint>();
        for(Checkpoint item: checkpointStorage) {
            if(validAgainstCommand(item, command)) {
                result.add(item);
            }
        }
        return result;
    }

    private static boolean validAgainstCommand(Checkpoint checkpoint, CheckpointCommand command) {
        boolean result = true;
        if(checkpoint!=null && command!=null) {
            if(command.getStartTime() > 0) {
                result = result && checkpoint.getTime()>command.getStartTime();
            }
            if(command.getEndTime() > 0) {
                result = result && checkpoint.getTime()<command.getEndTime();
            }

            if(command.getType() != null) {
                result = result && command.getType().equals(command.getType());
            }
        }
        return result;
    }

}
