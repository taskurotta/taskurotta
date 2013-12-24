package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.IncompleteProcessFinder;
import ru.taskurotta.service.recovery.RecoveryOperation;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 18.12.13
 * Time: 14:51
 */
public class HzIncompleteProcessFinder implements IncompleteProcessFinder {

    private static final Logger logger = LoggerFactory.getLogger(HzIncompleteProcessFinder.class);

    private IMap<UUID, Process> processIMap;

    private static final String START_TIME_INDEX_NAME = "startTime";
    private static final String STATE_INDEX_NAME = "state";

    public HzIncompleteProcessFinder(final HazelcastInstance hazelcastInstance, final OperationExecutor operationExecutor,
                                     String processesStorageMapName, final long findIncompleteProcessPeriod,
                                     final long incompleteTimeOutMillis, final String recoveryLockName,
                                     boolean enabled) {

        if (!enabled) {
            return;
        }

        this.processIMap = hazelcastInstance.getMap(processesStorageMapName);

        processIMap.addIndex(START_TIME_INDEX_NAME, true);
        processIMap.addIndex(STATE_INDEX_NAME, false);

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("HzIncompleteProcessFinderThread");
                return thread;
            }
        });
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {

                    ILock iLock = hazelcastInstance.getLock(recoveryLockName);
                    if (iLock.tryLock()) {
                        logger.debug("Get lock for find incomplete processes");
                    } else {
                        logger.debug("Can't get lock for find incomplete processes");
                        return;
                    }

                    long timeBefore = System.currentTimeMillis() - incompleteTimeOutMillis;

                    if (logger.isDebugEnabled()) {
                        logger.debug("Try to find incomplete processes, started before [{}]", new Date(timeBefore));
                    }

                    Predicate predicate = new Predicates.AndPredicate(
                            new Predicates.BetweenPredicate(START_TIME_INDEX_NAME, 0l, timeBefore),
                            new Predicates.EqualPredicate(STATE_INDEX_NAME, Process.START));

                    Collection<UUID> processIds = processIMap.keySet(predicate);

                    if (processIds == null || processIds.isEmpty()) {
                        logger.debug("Not found incomplete processes");
                        return;
                    } else {
                        logger.debug("Found [{}] incomplete processes", processIds.size());
                    }

                    do {
                        for (UUID processId : processIds) {
                            operationExecutor.enqueue(new RecoveryOperation(processId));
                            logger.trace("Send process [{}] to recovery", processId);
                        }

                        processIds = processIMap.keySet(predicate);

                    } while (processIds != null && !processIds.isEmpty());

                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        }, 0l, findIncompleteProcessPeriod, TimeUnit.MILLISECONDS);
    }
}
