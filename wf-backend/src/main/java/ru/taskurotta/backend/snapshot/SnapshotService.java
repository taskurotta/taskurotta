package ru.taskurotta.backend.snapshot;

import java.util.UUID;

/**
 * User: greg
 */
public interface SnapshotService {

    void createSnapshot(Snapshot snapshot);

    Snapshot getSnapshot(UUID snapshotId);
}
