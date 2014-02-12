package ru.taskurotta.service.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.executor.OperationExecutor;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Default logic for IncompleteProcessFinder processing. Single scheduled thread with node locking.
 * Injected DAOs should implement environment specific logic.
 * Date: 13.01.14 12:03
 */
public class DefaultIncompleteProcessFinder implements IncompleteProcessFinder {

    private static final Logger logger = LoggerFactory.getLogger(DefaultIncompleteProcessFinder.class);

    private OperationExecutor operationExecutor;
    private IncompleteProcessDao dao;
    private Lock nodeLock;
    private long findIncompleteProcessPeriod;
    private long incompleteTimeOutMillis;
    private boolean enabled;

    private ScheduledExecutorService scheduledExecutorService;

    public DefaultIncompleteProcessFinder(final IncompleteProcessDao dao, final Lock nodeLock, final OperationExecutor operationExecutor, long findIncompleteProcessPeriod,
                                      final long incompleteTimeOutMillis, boolean enabled) {

        this.operationExecutor = operationExecutor;
        this.dao = dao;
        this.nodeLock = nodeLock;
        this.findIncompleteProcessPeriod = findIncompleteProcessPeriod;
        this.incompleteTimeOutMillis = incompleteTimeOutMillis;
        this.enabled = enabled;

        if (enabled) {
            runProcessFinder();
        }

    }

    protected void runProcessFinder() {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("IncompleteProcessFinderThread");
                thread.setDaemon(true);
                return thread;
            }
        });

        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {

                    logger.debug("Fired incomplete process searcher, iteration period[{}] ms", findIncompleteProcessPeriod);

                    if (!operationExecutor.isEmpty()) { //has some processes previously recovered still in processing
                        logger.debug("RecoveryOperationExecutor queue isn't empty. Skip find incomplete processes");
                        return;
                    }

                    if (nodeLock.tryLock()) {
                        int recovered = 0;
                        long timeBefore = System.currentTimeMillis() - incompleteTimeOutMillis;

                        if (logger.isDebugEnabled()) {
                            logger.debug("Try to find incomplete processes started before [{}]", new Date(timeBefore));
                        }

                        Collection<UUID> incompleteProcesses = dao.findProcesses(timeBefore);
                        if (incompleteProcesses != null && !incompleteProcesses.isEmpty()) {
                            for (UUID ip : incompleteProcesses) {
                                toRecovery(ip);
                                recovered++;
                            }
                        }

                        logger.debug("[{}] processes were sent to recovery", recovered);

                    } else {
                        logger.debug("Can't get lock for incomplete processes search, skip iteration");
                    }

                } catch (Throwable e) {
                    logger.error("IncompleteProcessFinder iteration failed due to error, try to resume in [" + findIncompleteProcessPeriod + "] ms...", e);
                }
            }
        }, 0l, findIncompleteProcessPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public void toRecovery(UUID processId) {
        if (!enabled) {
            return;
        }

        operationExecutor.enqueue(new RecoveryOperation(processId));
        logger.trace("Process [{}] was sent to recovery queue", processId);
    }

    public void start() {
        this.enabled = true;

        if (scheduledExecutorService == null || scheduledExecutorService.isShutdown()) {
            runProcessFinder();
            logger.debug("DefaultIncompleteProcessFinder started");
        } else {
            logger.warn("DefaultIncompleteProcessFinder already started");
        }
    }

    public void stop() {
        this.enabled = false;

        if (scheduledExecutorService == null || scheduledExecutorService.isShutdown()) {
            logger.warn("DefaultIncompleteProcessFinder already stopped");
        } else {
            scheduledExecutorService.shutdown();
            logger.debug("DefaultIncompleteProcessFinder started");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
