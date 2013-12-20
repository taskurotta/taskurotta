package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.recovery.IncompleteProcessFinder;
import ru.taskurotta.service.recovery.RecoveryProcessService;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 18.12.13
 * Time: 16:09
 */
public class HzRecoveryProcessCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(HzRecoveryProcessCoordinator.class);

    public HzRecoveryProcessCoordinator(final HazelcastInstance hazelcastInstance, final IncompleteProcessFinder incompleteProcessFinder,
                                        final RecoveryProcessService recoveryProcessService, final long incompleteTimeOutMillis,
                                        final int incompleteProcessBatchSize, final long findIncompleteProcessPeriod,
                                        String recoveryProcessServiceName, final String recoveryProcessCoordinatorLockName) {

        final IExecutorService iExecutorService = hazelcastInstance.getExecutorService(recoveryProcessServiceName);

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                ILock iLock = hazelcastInstance.getLock(recoveryProcessCoordinatorLockName);
                if (!iLock.tryLock()) {
                    return;
                }

                Collection<UUID> incompleteProcessIds = incompleteProcessFinder.find(incompleteTimeOutMillis, incompleteProcessBatchSize);
                if (incompleteProcessIds.isEmpty()) {
                    return;
                }

                for (final UUID uuid : incompleteProcessIds) {

                    Future<Boolean> future = iExecutorService.submitToKeyOwner(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return recoveryProcessService.restartProcess(uuid);
                        }
                    }, uuid);

                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("Catch error while get result of restart process [" + uuid + "]", e);
                    }
                }
            }
        }, 0l, findIncompleteProcessPeriod, TimeUnit.MILLISECONDS);
    }
}
