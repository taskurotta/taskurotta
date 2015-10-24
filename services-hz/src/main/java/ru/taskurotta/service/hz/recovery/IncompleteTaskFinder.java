package ru.taskurotta.service.hz.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.hz.TaskKey;
import ru.taskurotta.service.recovery.RecoveryService;
import ru.taskurotta.service.recovery.RecoveryThreads;
import ru.taskurotta.service.storage.TaskDao;
import ru.taskurotta.util.Shutdown;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * User: stukushin
 * Date: 11.06.2015
 * Time: 14:52
 */

public class IncompleteTaskFinder implements RecoveryThreads {

    private static final Logger logger = LoggerFactory.getLogger(IncompleteTaskFinder.class);

    private RecoveryService recoveryService;
    private TaskDao taskDao;
    private long incompleteTaskFindTimeout;
    private int batchSize;
    private Lock nodeLock;

    private AtomicBoolean enabled = new AtomicBoolean(false);
    private ScheduledExecutorService executorService;

    public IncompleteTaskFinder(RecoveryService recoveryService, TaskDao taskDao, long incompleteTaskFindTimeout,
                                int batchSize, Lock nodeLock, boolean enabled) {
        this.recoveryService = recoveryService;
        this.taskDao = taskDao;
        this.incompleteTaskFindTimeout = incompleteTaskFindTimeout;
        this.batchSize = batchSize;
        this.nodeLock = nodeLock;

        this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "IncompleteTaskFinder");
                thread.setDaemon(true);
                return thread;
            }
        });
        Shutdown.addHook(executorService);

        if (enabled) {
            start();
        } else {
            logger.warn("Recovery service IncompleteTaskFinder is disabled.");
        }
    }

    @Override
    public void start() {
        if (!enabled.compareAndSet(false, true)) {
            // already started
            return;
        }

        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (Shutdown.isTrue() || !isStarted()) {
                    logger.debug("System shutdown or service is disabled");
                    return;
                }

                try {
                    if (nodeLock.tryLock()) {
                        try (ResultSetCursor incompleteTasksCursor = taskDao.findIncompleteTasks(System.currentTimeMillis(), batchSize)) {
                            while (true) {
                                Collection<TaskKey> incompleteTasks = incompleteTasksCursor.getNext();

                                if (incompleteTasks.isEmpty()) {
                                    logger.debug("Incomplete tasks not found");
                                    break;
                                }

                                for (TaskKey taskKey : incompleteTasks) {
                                    recoveryService.reenqueueTask(taskKey.getTaskId(), taskKey.getProcessId());
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    logger.error("IncompleteTaskFinder iteration failed due to error, try to resume in [" + incompleteTaskFindTimeout + "] ms...", e);
                }
            }
        }, 0l, incompleteTaskFindTimeout, TimeUnit.MILLISECONDS);
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
