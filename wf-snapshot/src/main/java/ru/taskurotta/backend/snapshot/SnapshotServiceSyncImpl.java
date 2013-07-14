package ru.taskurotta.backend.snapshot;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
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
    private HazelcastInstance hazelcastInstance;

    public SnapshotServiceSyncImpl(SnapshotDataSource dataSource, GeneralTaskServer generalTaskServer, HazelcastInstance hazelcastInstance) {
        this.dataSource = dataSource;
        this.generalTaskServer = generalTaskServer;
        this.hazelcastInstance = hazelcastInstance;
        validateDependencies();
    }

    private void validateDependencies() {
        if (dataSource == null) {
            throw new IllegalStateException("Snapshot dataSource is null :( ");
        }
        if (generalTaskServer == null) {
            throw new IllegalStateException("General task server is null :( ");
        }
        if (hazelcastInstance == null){
            throw new IllegalStateException("Hazelcust instance is null :( ");
        }
    }

    @Override
    public void createSnapshot(UUID processID) {
        logger.trace("Snapshot creating");
        final Graph graph = generalTaskServer.getDependencyBackend().getGraph(processID);
        final ILock lock = hazelcastInstance.getLock(graph);
        lock.lock();
        final Graph copy = graph.copy();
        lock.unlock();
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
