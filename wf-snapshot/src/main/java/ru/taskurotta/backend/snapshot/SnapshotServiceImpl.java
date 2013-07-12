package ru.taskurotta.backend.snapshot;

import com.hazelcast.config.Config;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.snapshot.datasource.SnapshotDataSource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * User: greg
 */
public class SnapshotServiceImpl implements SnapshotService {

    private final static Logger logger = LoggerFactory.getLogger(SnapshotServiceImpl.class);
    private final SnapshotDataSource dataSource;
    private final BlockingQueue<UUID> queue;
    private HazelcastInstance hazelcastInstance;

    public SnapshotServiceImpl(SnapshotDataSource dataSource, HazelcastInstance hazelcastInstance) {
        this.dataSource = dataSource;
        this.hazelcastInstance = hazelcastInstance;
        queue = hazelcastInstance.getQueue("snapshotQueue");
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (queue.size() > 0) {
                            final UUID processId = queue.poll();
                            final SnapshotSaveTask snapshotSaveTask = new SnapshotSaveTask(processId);
                            final ExecutorService executorService = getHazelcastInstance().getExecutorService();
                            final DistributedTask<Void> task = new DistributedTask<>(snapshotSaveTask);
                            task.setExecutionCallback(new ExecutionCallback<Void>() {
                                @Override
                                public void done(Future<Void> future) {
                                    logger.trace("Snapshot saved to repository");
                                }
                            });
                            executorService.submit(task);
                        }
                    } catch (Exception ex) {
                        logger.error("error", ex);
                    }
                }
            }
        });
    }

    @Override
    public void createSnapshot(UUID processID) {
        queue.add(processID);
    }

    /**
     * For tests ONLY
     *
     * @return HazelcastInstance
     */
    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
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
    }

}
