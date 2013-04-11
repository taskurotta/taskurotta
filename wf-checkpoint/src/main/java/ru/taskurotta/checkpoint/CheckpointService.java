package ru.taskurotta.checkpoint;

import java.util.List;

import ru.taskurotta.checkpoint.model.Checkpoint;
import ru.taskurotta.checkpoint.model.CheckpointCommand;

public interface CheckpointService {

    public void addCheckpoint(Checkpoint checkpoint);

    public void addCheckpoints(List<Checkpoint> checkpoints);

    public void removeCheckpoint(Checkpoint checkpoint);

    public void removeCheckpoints(List<Checkpoint> checkpoint);

    public List<Checkpoint> listCheckpoints(CheckpointCommand command);

}
