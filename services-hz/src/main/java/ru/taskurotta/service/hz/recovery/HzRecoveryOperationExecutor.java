package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;
import ru.taskurotta.service.executor.Operation;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.RecoveryProcessService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 24.12.13
 * Time: 11:39
 */
public class HzRecoveryOperationExecutor implements OperationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(HzRecoveryOperationExecutor.class);

    private boolean enabled;

    private IQueue<Operation> operationIQueue;

    public HzRecoveryOperationExecutor(HazelcastInstance hazelcastInstance, final RecoveryProcessService recoveryProcessService,
                                       String recoveryOperationQueueName,int recoveryOperationPoolSize, boolean enabled) {
        this(hazelcastInstance, recoveryProcessService, null, recoveryOperationQueueName, recoveryOperationPoolSize, enabled);
    }

    public HzRecoveryOperationExecutor(HazelcastInstance hazelcastInstance, final RecoveryProcessService recoveryProcessService,
                                       HzQueueConfigSupport hzQueueConfigSupport, String recoveryOperationQueueName,
                                       int recoveryOperationPoolSize, boolean enabled) {

        this.enabled = enabled;
        if (!enabled) {
            return;
        }

        if (hzQueueConfigSupport != null) {
            hzQueueConfigSupport.createQueueConfig(recoveryOperationQueueName);
        } else {
            logger.warn("HzQueueConfigSupport is not configured");
        }

        this.operationIQueue = hazelcastInstance.getQueue(recoveryOperationQueueName);

        final ExecutorService recoveryOperationExecutorService = Executors.newFixedThreadPool(recoveryOperationPoolSize);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("HzRecoveryOperationExecutorThread");
                thread.setDaemon(true);
                return thread;
            }
        });

        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                while (!operationIQueue.isEmpty()) {
                    try {
                        Operation operation = operationIQueue.poll(1, TimeUnit.SECONDS);

                        if (operation != null) {
                            operation.init(recoveryProcessService);
                            recoveryOperationExecutorService.submit(operation);
                        }
                    } catch (Throwable throwable) {
                        logger.error(throwable.getLocalizedMessage(), throwable);
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void enqueue(Operation operation) {
        if (!enabled) {
            return;
        }
        operationIQueue.offer(operation);
    }

    @Override
    public int size() {
        if (!enabled) {
            return 0;
        }
        return operationIQueue.size();
    }

    @Override
    public boolean isEmpty() {
        return !enabled || operationIQueue.isEmpty();
    }
}
