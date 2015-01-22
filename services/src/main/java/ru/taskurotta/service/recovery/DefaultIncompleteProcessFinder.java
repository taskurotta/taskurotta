package ru.taskurotta.service.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.executor.OperationExecutor;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

/**
 * Default logic for IncompleteProcessFinder processing. Single scheduled thread with node locking.
 * Injected DAOs should implement environment specific logic.
 * Date: 13.01.14 12:03
 */
public class DefaultIncompleteProcessFinder implements IncompleteProcessFinder {

    private static final Logger logger = LoggerFactory.getLogger(DefaultIncompleteProcessFinder.class);

    public static AtomicInteger processesOnTimeoutFoundedCounter = new AtomicInteger(0);

    private OperationExecutor operationExecutor;
    private IncompleteProcessDao dao;
    private Lock nodeLock;
    private long findIncompleteProcessPeriod;
    private long incompleteTimeOutMillis;
    private AtomicBoolean enabled = new AtomicBoolean(false);
    private int batchSize;

    public DefaultIncompleteProcessFinder(final IncompleteProcessDao dao, final Lock nodeLock, final OperationExecutor operationExecutor, long findIncompleteProcessPeriod,
                                          final long incompleteTimeOutMillis, boolean enabled, int batchSize) {

        this.operationExecutor = operationExecutor;
        this.dao = dao;
        this.nodeLock = nodeLock;
        this.findIncompleteProcessPeriod = findIncompleteProcessPeriod;
        this.incompleteTimeOutMillis = incompleteTimeOutMillis;
        this.batchSize = batchSize;

        if (enabled) {
            start();
        } else {
            logger.warn("Recovery service Incomplete Process Finder is disabled.");
        }

    }

    @Override
    public void start() {

        if (!enabled.compareAndSet(false, true)) {
            // already started
            return;
        }

        // todo: more then one threads may be running after fastest stop() and start() invocation. Current thread
        // may be sleeping and can not catch stop signal from shared atomic long "enabled"
        Thread processFinder = new Thread(new Runnable() {
            @Override
            public void run() {

                while (enabled.get() && !Thread.currentThread().isInterrupted()) {

                    try {

                        TimeUnit.MILLISECONDS.sleep(findIncompleteProcessPeriod);

                        logger.debug("Fired incomplete process searcher, iteration period[{}] ms", findIncompleteProcessPeriod);

                        //has some processes previously recovered still in processing
                        if (!operationExecutor.isEmpty()) {
                            logger.debug("RecoveryOperationExecutor queue isn't empty. Skip find incomplete processes");
                            continue;
                        }

                        if (nodeLock.tryLock()) {

                            long timeBefore = System.currentTimeMillis() - incompleteTimeOutMillis;

                            if (logger.isDebugEnabled()) {
                                logger.debug("Try to find incomplete processes started before [{}]", new Date(timeBefore));
                            }

                            try (IncompleteProcessesCursor incompleteProcessesCursor = dao.findProcesses(timeBefore,
                                    batchSize)) {

                                while (true) {
                                    Collection<UUID> incompleteProcesses = incompleteProcessesCursor.getNext();

                                    if (incompleteProcesses.isEmpty()) {
                                        break;
                                    }

                                    processesOnTimeoutFoundedCounter.addAndGet(incompleteProcesses.size());

                                    for (UUID ip : incompleteProcesses) {
                                        toRecovery(ip);
                                    }

                                    logger.debug("[{}] processes were sent to recovery", incompleteProcesses.size());
                                }

                            }

                        } else {
                            logger.debug("Can't get lock for incomplete processes search, skip iteration");
                        }

                    } catch (Throwable e) {
                        logger.error("IncompleteProcessFinder iteration failed due to error, try to resume in [" + findIncompleteProcessPeriod + "] ms...", e);
                    }
                }
            }
        });

        processFinder.setName("IncompleteProcessFinder");
        processFinder.setDaemon(true);

        processFinder.start();

    }

    @Override
    public void toRecovery(UUID processId) {
        if (enabled.get()) {
            operationExecutor.enqueue(new RecoveryOperation(processId));
            logger.trace("Process [{}] was sent to recovery queue", processId);
        }
    }

    @Override
    public boolean isStarted() {
        return enabled.get();
    }

    public void stop() {
        enabled.set(false);
    }

    public boolean isEnabled() {
        return enabled.get();
    }
}
