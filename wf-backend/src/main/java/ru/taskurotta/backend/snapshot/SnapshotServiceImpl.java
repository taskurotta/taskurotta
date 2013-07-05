package ru.taskurotta.backend.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.snapshot.datasource.SnapshotDataSource;

import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * User: greg
 */
public class SnapshotServiceImpl implements SnapshotService {

    private final static Logger logger = LoggerFactory.getLogger(SnapshotServiceImpl.class);

    private final SnapshotDataSource dataSource;

    private final BlockingDeque<Snapshot> deque;

    public SnapshotServiceImpl(SnapshotDataSource dataSource) {
        this.dataSource = dataSource;
        this.deque = new LinkedBlockingDeque<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new TaskToSave(dataSource, deque));
    }

    @Override
    public void createSnapshot(Snapshot snapshot) {
        try {
            deque.put(snapshot);
            logger.trace("Snapshot putted to deque: " + snapshot);
        } catch (InterruptedException e) {
            logger.error("error on create snapshot", e);
        }
    }

    @Override
    public Snapshot getSnapshot(UUID snapshotId) {
        return dataSource.loadSnapshotById(snapshotId);
    }

    public static class TaskToSave implements Runnable {

        private final SnapshotDataSource dataSource;
        private final BlockingDeque<Snapshot> deque;

        public TaskToSave(SnapshotDataSource dataSource, BlockingDeque<Snapshot> deque) {
            this.dataSource = dataSource;
            this.deque = deque;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (deque.size() > 0) {
                        Snapshot sn = deque.pop();
                        dataSource.save(sn);
                        logger.trace("Snapshot saved to repository: " + sn);
                    }
                } catch (Exception ex) {
                    logger.error("error", ex);
                }
            }
        }
    }
}
