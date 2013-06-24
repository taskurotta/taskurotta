package ru.taskurotta.backend.snapshot;

import java.util.UUID;

/**
 * User: greg
 */
public class SnapshotServiceImpl implements SnapshotService {

    private SnapshotDataSource dataSource;

    public SnapshotServiceImpl(SnapshotDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createSnapshot(Snapshot snapshot) {
        validateSnapshot(snapshot);
        dataSource.save(snapshot);
    }

    @Override
    public Snapshot getSnapshot(UUID snapshotId) {
        return dataSource.loadSnapshotById(snapshotId);
    }

    private void validateSnapshot(Snapshot snapshot) {
        if (snapshot.getGraph() == null) {
            throw new IllegalStateException("graph property is null");
        }
        if (snapshot.getTask() == null) {
            throw new IllegalStateException("task property is null");
        }
        if (snapshot.getTaskDecision() == null) {
            throw new IllegalStateException("task decision property is null");
        }
    }


}
