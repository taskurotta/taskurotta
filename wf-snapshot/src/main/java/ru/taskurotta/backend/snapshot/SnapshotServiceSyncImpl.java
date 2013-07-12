package ru.taskurotta.backend.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.snapshot.datasource.SnapshotDataSource;
import ru.taskurotta.server.GeneralTaskServer;

import java.util.List;
import java.util.UUID;

/**
 * User: greg
 * Snapshot sync service implmentation
 */
public class SnapshotServiceSyncImpl implements SnapshotService {
    private final static Logger logger = LoggerFactory.getLogger(SnapshotServiceAsyncImpl.class);
    private final SnapshotDataSource dataSource;
    private final GeneralTaskServer generalTaskServer;

    public SnapshotServiceSyncImpl(SnapshotDataSource dataSource, GeneralTaskServer generalTaskServer) {
        this.dataSource = dataSource;
        this.generalTaskServer = generalTaskServer;
        validateDependencies();
    }

    private void validateDependencies() {
        if (dataSource == null) {
            throw new IllegalStateException("Snapshot dataSource is null :( ");
        }
        if (generalTaskServer == null) {
            throw new IllegalStateException("General task server is null :( ");
        }
    }

    @Override
    public void createSnapshot(UUID processID) {
        logger.trace("Snapshot creating");
        final Graph graph = generalTaskServer.getDependencyBackend().getGraph(processID);
        final Graph copy = graph.copy();
        final Snapshot snapshot = new Snapshot();
        snapshot.setGraph(copy);
        snapshot.setSnapshotId(UUID.randomUUID());
        snapshot.setProcessId(processID);
        saveSnapshot(snapshot);
    }


    @Override
    public Snapshot getSnapshot(UUID snapshotId) {
        return dataSource.loadSnapshotById(snapshotId);
    }

    @Override
    public List<Snapshot> getSnapshotByProcessId(UUID processId) {
        return dataSource.loadSnapshotsByProccessId(processId);
    }

    @Override
    public void saveSnapshot(Snapshot snapshot) {
        dataSource.save(snapshot);
        logger.trace("Snapshot saved");
    }
}
