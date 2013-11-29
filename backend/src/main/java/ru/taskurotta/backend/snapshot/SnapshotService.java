package ru.taskurotta.backend.snapshot;

import java.util.List;
import java.util.UUID;

/**
 * User: greg
 */
public interface SnapshotService {


    void createSnapshot(UUID processID);

    Snapshot getSnapshot(UUID snapshotId);

    List<Snapshot> getSnapshotByProcessId(UUID snapshotId);

    void saveSnapshot(Snapshot snapshot);
}
