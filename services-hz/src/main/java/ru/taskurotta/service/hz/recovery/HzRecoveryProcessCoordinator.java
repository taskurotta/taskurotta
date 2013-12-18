package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.recovery.IncompleteProcessFinder;
import ru.taskurotta.service.recovery.RecoveryProcessService;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 18.12.13
 * Time: 16:09
 */
public class HzRecoveryProcessCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(HzRecoveryProcessCoordinator.class);

    private BlockingQueue<UUID> uuidQueue = new LinkedBlockingQueue<>();

    public HzRecoveryProcessCoordinator(final HazelcastInstance hazelcastInstance, final IncompleteProcessFinder incompleteProcessFinder,
                                        final RecoveryProcessService recoveryProcessService, final long inactiveTimeOutMillis,
                                        long findIncompleteProcessDuration, int recoveryTaskPoolSize) {

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Collection<UUID> incompleteProcessIds = incompleteProcessFinder.find(inactiveTimeOutMillis);

                if (incompleteProcessIds == null || incompleteProcessIds.isEmpty()) {
                    return;
                }

                for (UUID uuid : incompleteProcessIds) {
                    if (isLocalItem(uuid)) {
                        uuidQueue.add(uuid);
                    }
                }
            }

            private boolean isLocalItem(UUID id) {
                Partition partition = hazelcastInstance.getPartitionService().getPartition(id);
                return partition.getOwner().localMember();
            }
        }, 0l, findIncompleteProcessDuration, TimeUnit.MILLISECONDS);

        ExecutorService executorService = Executors.newFixedThreadPool(recoveryTaskPoolSize);
        for (int i = 0; i < recoveryTaskPoolSize; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            UUID processId = uuidQueue.take();
                            recoveryProcessService.restartProcess(processId);
                        } catch (InterruptedException e) {
                            logger.error("Catch exception while get processId for start recovery", e);
                        }
                    }
                }
            });
        }
    }
}
