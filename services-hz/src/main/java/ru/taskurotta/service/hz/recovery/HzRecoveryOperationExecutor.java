package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private IQueue<Operation> operationIQueue;

    public HzRecoveryOperationExecutor(HazelcastInstance hazelcastInstance, final String recoveryOperationQueueName,
                                       final RecoveryProcessService recoveryProcessService, final int recoveryOperationPoolSize) {
        this.operationIQueue = hazelcastInstance.getQueue(recoveryOperationQueueName);

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
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {

                    while (true) {
                        Operation operation = operationIQueue.poll(1, TimeUnit.SECONDS);

                        if (operation == null) {
                            continue;
                        }

                        operation.init(recoveryProcessService);

                        recoveryOperationExecutorService.submit(operation);
                    }

                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        });
    }

    @Override
    public void enqueue(Operation operation) {
        try {
            operationIQueue.offer(operation, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Catch exception while offer operation to queue", e);
        }
    }

    @Override
    public int size() {
        return operationIQueue.size();
    }

    @Override
    public boolean isEmpty() {
        return operationIQueue.isEmpty();
    }
}
