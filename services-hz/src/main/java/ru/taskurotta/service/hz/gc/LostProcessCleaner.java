package ru.taskurotta.service.hz.gc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.common.ResultSetCursor;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

/**
 * User: stukushin
 * Date: 06.10.2015
 * Time: 16:41
 */

public class LostProcessCleaner implements GarbageCollectorThread {

    private static final Logger logger = LoggerFactory.getLogger(LostProcessCleaner.class);

    private ProcessService processService;
    private GarbageCollectorService garbageCollectorService;
    private long lostProcessFindTimeout;
    private int batchSize;
    private long timeBeforeDeleteFinishedProcess;
    private long timeBeforeDeleteAbortedProcess;
    private AtomicBoolean enabled = new AtomicBoolean(false);
    private Lock nodeLock;

    private ScheduledExecutorService scheduledExecutorService;

    public static AtomicInteger cleanedProcessesCounter = new AtomicInteger();

    public LostProcessCleaner(ProcessService processService, GarbageCollectorService garbageCollectorService,
                              long lostProcessFindTimeout, int batchSize, long timeBeforeDeleteFinishedProcess,
                              long timeBeforeDeleteAbortedProcess, boolean enabled, boolean gcEnabled, Lock nodeLock) {
        this.processService = processService;
        this.garbageCollectorService = garbageCollectorService;
        this.lostProcessFindTimeout = lostProcessFindTimeout;
        this.batchSize = batchSize;
        this.timeBeforeDeleteFinishedProcess = timeBeforeDeleteFinishedProcess;
        this.timeBeforeDeleteAbortedProcess = timeBeforeDeleteAbortedProcess;
        this.enabled.set(enabled);
        this.nodeLock = nodeLock;

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("LostProcessCleaner");
            return thread;
        });
        Shutdown.addHook(scheduledExecutorService);

        if (enabled && gcEnabled) {
            start();
        } else {
            logger.warn("Lost process cleaner is disabled or garbage collector is disabled.");
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
                    long now = System.currentTimeMillis();
                    long lastFinishedProcessDeleteTime = now - timeBeforeDeleteFinishedProcess;
                    long lastAbortedProcessDeleteTime = now - timeBeforeDeleteAbortedProcess;
                    try (ResultSetCursor<UUID> cursor = processService.findLostProcesses(
                            lastFinishedProcessDeleteTime, lastAbortedProcessDeleteTime, batchSize)) {
                        Collection<UUID> processIds;
                        while (!(processIds = cursor.getNext()).isEmpty()) {
                            logger.debug("Found [{}] lost processes", processIds.size());

                            for (UUID processId : processIds) {
                                // delete immediately
                                garbageCollectorService.collect(processId, 0L);
                            }

                            cleanedProcessesCounter.addAndGet(processIds.size());
                        }
                    }
                }
            } catch (Throwable e) {
                logger.error("LostProcessCleaner iteration failed due to error, try to resume in [" + lostProcessFindTimeout + "] ms...", e);
            } finally {
                nodeLock.unlock();
            }
        }, 0L, lostProcessFindTimeout, TimeUnit.MILLISECONDS);
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
