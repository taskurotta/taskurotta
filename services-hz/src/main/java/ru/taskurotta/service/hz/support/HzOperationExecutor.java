package ru.taskurotta.service.hz.support;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.service.executor.Operation;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.util.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created on 23.04.2015.
 */
public class HzOperationExecutor<T> implements OperationExecutor<T> {

    private static final Logger logger = LoggerFactory.getLogger(HzOperationExecutor.class);

    private boolean enabled;

    private CachedQueue<Operation<T>> operationIQueue;
    private BlockingQueue<Runnable> localOperationQueue;

    public HzOperationExecutor(final T nativePoint, final HazelcastInstance hzInstance, final HzQueueConfigSupport hzQueueConfigSupport, String queueName, int poolSize, boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            return;
        }

        if (hzQueueConfigSupport != null) {
            hzQueueConfigSupport.createQueueConfig(queueName);
        } else {
            logger.warn("HzQueueConfigSupport is not set for HzOperationExecutor");
        }

        this.operationIQueue = hzInstance.getDistributedObject(CachedQueue.class.getName(), queueName);
        this.localOperationQueue = new ArrayBlockingQueue<Runnable>(poolSize * 3) {
            @Override
            public boolean offer(Runnable runnable) {
                try {
                    super.put(runnable);//to block thread on queue is full
                    return true;
                } catch (InterruptedException e) {
                    return false;
                }
            }
        };

        final ExecutorService operationExecutorService = new ThreadPoolExecutor(poolSize, poolSize,
                0L, TimeUnit.MILLISECONDS,
                localOperationQueue, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("HzOperationExecutorThread::worker");
                thread.setDaemon(true);
                return thread;
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread (Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("HzOperationExecutorThread::planner");
                thread.setDaemon(true);
                return thread;
            }
        });

        executorService.submit(new Runnable() {
            @Override
            public void run() {

                while (!Shutdown.isTrue()) {

                    try {
                        Operation<T> operation = operationIQueue.poll(1, TimeUnit.SECONDS);

                        if (operation == null) {
                            continue;
                        }

                        operation.init(nativePoint);

                        operationExecutorService.submit(operation);

                    } catch (Throwable throwable) {
                        logger.error(throwable.getLocalizedMessage(), throwable);
                    }
                }
            }
        });
    }


    @Override
    public void enqueue (Operation<T> operation) {
        if (!enabled) {
            return;
        }
        operationIQueue.offer(operation);
    }

    @Override
    public int size () {
        if (!enabled) {
            return 0;
        }
        return operationIQueue.size();
    }

    @Override
    public boolean isEmpty () {
        return !enabled || (operationIQueue.isEmpty() && localOperationQueue.isEmpty());
    }
}
