package ru.taskurotta.backend.snapshot;

import java.util.UUID;

/**
 * User: greg
 */
public interface SnapshotService {


    void createSnapshot(UUID processID);

    Snapshot getSnapshot(UUID snapshotId);

    void saveSnapshot(Snapshot snapshot);
}
