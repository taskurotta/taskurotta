package ru.taskurotta.backend.snapshot;

import com.hazelcast.core.ILock;
import com.hazelcast.core.PartitionAware;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * User: greg
 */
public class SnapshotSaveTask implements Callable<UUID>, PartitionAware, Serializable {

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
    public UUID call() throws Exception {
        final HazelcastTaskServer taskServer = HazelcastTaskServer.getInstance();
        final Graph graph = taskServer.getDependencyBackend().getGraph(processId);
        ILock lock = taskServer.getHzInstance().getLock(graph);
        lock.lock();
        final Graph copyGraph = graph.copy();
        lock.unlock();
        final Snapshot snapshot = new Snapshot();
        snapshot.setGraph(copyGraph);
        snapshot.setSnapshotId(UUID.randomUUID());
        taskServer.getSnapshotService().saveSnapshot(snapshot);
        return snapshot.getSnapshotId();
    }

    @Override

    public Object getPartitionKey() {
        return processId;
    }
}