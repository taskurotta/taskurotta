package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.service.executor.Operation;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.RecoveryProcessService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private CachedQueue<Operation> operationIQueue;

    private volatile boolean notInShutdown = true;

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

        this.operationIQueue = hazelcastInstance.getDistributedObject(CachedQueue.class.getName(), recoveryOperationQueueName);

        final ExecutorService recoveryOperationExecutorService = Executors.newFixedThreadPool(recoveryOperationPoolSize);

        ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("HzRecoveryOperationExecutorThread");
                thread.setDaemon(true);
                return thread;
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                notInShutdown = false;
            }
        });

        executorService.submit(new Runnable() {
            @Override
            public void run() {

                while (notInShutdown) {

                    try {
                        Operation operation = operationIQueue.poll(1, TimeUnit.SECONDS);

                        if (operation == null) {
                            continue;
                        }

                        operation.init(recoveryProcessService);

                        recoveryOperationExecutorService.submit(operation);

                    } catch (Throwable throwable) {
                        logger.error(throwable.getLocalizedMessage(), throwable);
                    }
                }

            }
        });
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
