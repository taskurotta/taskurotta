package ru.taskurotta.backend.snapshot;

import ru.taskurotta.backend.snapshot.datasource.SnapshotDataSource;

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
        dataSource.save(snapshot);
    }

    @Override
    public Snapshot getSnapshot(UUID snapshotId) {
        return dataSource.loadSnapshotById(snapshotId);
    }

}
