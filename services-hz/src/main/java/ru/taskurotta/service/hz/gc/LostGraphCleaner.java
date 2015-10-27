package ru.taskurotta.service.hz.gc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.gc.GarbageCollectorThread;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.util.Shutdown;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * User: stukushin
 * Date: 07.10.2015
 * Time: 17:00
 */

public class LostGraphCleaner implements GarbageCollectorThread {

    private static final Logger logger = LoggerFactory.getLogger(LostGraphCleaner.class);

    private GraphDao graphDao;
    private ProcessService processService;
    private GarbageCollectorService garbageCollectorService;
    private long lostGraphFindTimeout;
    private int batchSize;
    private long incompleteProcessTimeout;
    private AtomicBoolean enabled = new AtomicBoolean(false);
    private Lock nodeLock;

    private ScheduledExecutorService scheduledExecutorService;

    public LostGraphCleaner(GraphDao graphDao, ProcessService processService,
                            GarbageCollectorService garbageCollectorService, long lostGraphFindTimeout, int batchSize,
                            long incompleteProcessTimeout, boolean enabled, boolean gcEnabled, Lock nodeLock) {
        this.graphDao = graphDao;
        this.processService = processService;
        this.garbageCollectorService = garbageCollectorService;
        this.lostGraphFindTimeout = lostGraphFindTimeout;
        this.batchSize = batchSize;
        this.incompleteProcessTimeout = incompleteProcessTimeout;
        this.enabled.set(enabled);
        this.nodeLock = nodeLock;

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("LostGraphCleaner");
            return thread;
        });
        Shutdown.addHook(scheduledExecutorService);

        if (enabled && gcEnabled) {
            start();
        } else {
            logger.warn("Lost graph cleaner is disabled or garbage collector is disabled.");
        }
    }

    @Override
    public void start() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!enabled.get()) {
                logger.warn("Lost process cleaner is disabled!");
                return;
            }

            try {
                if (nodeLock.tryLock()) {
                    long lastGraphChangeTime = System.currentTimeMillis() - incompleteProcessTimeout;
                    try (ResultSetCursor<UUID> cursor = graphDao.findLostGraphs(lastGraphChangeTime, batchSize)) {
                        Collection<UUID> graphIds;
                        while (!(graphIds = cursor.getNext()).isEmpty()) {
                            logger.debug("Found [{}] lost graphs", graphIds.size());

                            for (UUID graphId : graphIds) {
                                Process process = processService.getProcess(graphId);
                                if (process == null) {
                                    // delete immediately
                                    garbageCollectorService.collect(graphId, 0L);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                logger.error("LostGraphCleaner iteration failed due to error, try to resume in [" + lostGraphFindTimeout + "] ms...", e);
            } finally {
                nodeLock.unlock();
            }
        }, 0L, lostGraphFindTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        enabled.set(false);
    }

    @Override
    public boolean isStarted() {
        return enabled.get();
    }
}
