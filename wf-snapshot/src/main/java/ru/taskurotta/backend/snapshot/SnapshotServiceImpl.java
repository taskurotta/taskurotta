package ru.taskurotta.backend.snapshot;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.snapshot.datasource.SnapshotDataSource;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: greg
 */
public class SnapshotServiceImpl implements SnapshotService {

    private final static Logger logger = LoggerFactory.getLogger(SnapshotServiceImpl.class);
    private final SnapshotDataSource dataSource;
    private final BlockingQueue<UUID> queue;
    private HazelcastInstance hazelcastInstance;

    public SnapshotServiceImpl(SnapshotDataSource dataSource) {
        this.dataSource = dataSource;
        Config cfg = new Config();
        hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
        queue = hazelcastInstance.getQueue("snapshotQueue");
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (queue.size() > 0) {
                            final UUID processId = queue.poll();
                            final ExecutorService executorService = getHazelcastInstance().getExecutorService();
                            executorService.submit(new TaskToSave(processId));
                            logger.trace("Snapshot saved to repository: " + processId);
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

    private HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    @Override
    public Snapshot getSnapshot(UUID snapshotId) {
        return dataSource.loadSnapshotById(snapshotId);
    }

}
