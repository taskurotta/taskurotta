package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.service.executor.Operation;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.RecoveryService;
import ru.taskurotta.util.Shutdown;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
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
    private BlockingQueue<Runnable> localOperationQueue;

    public HzRecoveryOperationExecutor(HazelcastInstance hazelcastInstance, final RecoveryService recoveryService,
                                       String recoveryOperationQueueName, int recoveryOperationPoolSize, boolean enabled) {
        this(hazelcastInstance, recoveryService, null, recoveryOperationQueueName, recoveryOperationPoolSize, enabled);
    }

    public HzRecoveryOperationExecutor(HazelcastInstance hazelcastInstance, final RecoveryService recoveryService,
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
        this.localOperationQueue = new ArrayBlockingQueue<Runnable>(recoveryOperationPoolSize * 3) {
            @Override
            public boolean offer(Runnable runnable) {
                try {
                    super.put(runnable);
                    return true;
                } catch (InterruptedException e) {
                    return false;
                }
            }
        };

        final ExecutorService recoveryOperationExecutorService = new ThreadPoolExecutor(recoveryOperationPoolSize, recoveryOperationPoolSize,
                0L, TimeUnit.MILLISECONDS,
                localOperationQueue, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("HzRecoveryOperationExecutorThread::worker");
                thread.setDaemon(true);
                return thread;
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("HzRecoveryOperationExecutorThread::planner");
                thread.setDaemon(true);
                return thread;
            }
        });

        executorService.submit(new Runnable() {
            @Override
            public void run() {

                while (!Shutdown.isTrue()) {

                    try {
                        Operation operation = operationIQueue.poll(1, TimeUnit.SECONDS);

                        if (operation == null) {
                            continue;
                        }

                        operation.init(recoveryService);

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
        return !enabled || (operationIQueue.isEmpty() && localOperationQueue.isEmpty());
    }


}
