package ru.taskurotta.checkpoint;

import java.util.List;

public interface CheckpointService {

    public void addCheckpoint(Checkpoint checkpoint);

    public void removeCheckpoint(Checkpoint checkpoint);

    public List<Checkpoint> listCheckpoints(CheckpointCommand command);

}
