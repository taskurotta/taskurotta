package ru.taskurotta.backend.snapshot;

import com.hazelcast.core.ILock;
import com.hazelcast.core.PartitionAware;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;
import ru.taskurotta.backend.snapshot.datasource.JDBCSnapshotDataSource;
import ru.taskurotta.backend.snapshot.datasource.SnapshotDataSource;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * User: greg
 */
public class SnapshotSaveTask implements Callable<Snapshot>, PartitionAware, Serializable {

    private UUID processId;

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public SnapshotSaveTask() {

    }

    public SnapshotSaveTask(UUID processId) {
        this.processId = processId;
    }

    @Override
    public Snapshot call() throws Exception {
        final HazelcastTaskServer taskServer = HazelcastTaskServer.getInstance();
        Graph graph = taskServer.getDependencyBackend().getGraph(processId);
        ILock lock = taskServer.getHzInstance().getLock(graph);
        Graph copyGraph = graph.copy();
        lock.unlock();
        Snapshot snapshot = new Snapshot();
        snapshot.setGraph(copyGraph);
        snapshot.setSnapshotId(UUID.randomUUID());
        return snapshot;
    }

    @Override

    public Object getPartitionKey() {
        return processId;
    }
}