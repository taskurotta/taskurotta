package ru.taskurotta.service.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.util.EmptyStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.service.hz.TaskKey;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 */
public class PendingDecisionQueueProxy {

    private static final Logger logger = LoggerFactory.getLogger(PendingDecisionQueueProxy.class);

    public static final long LOCK_TRY_TIME_MLS = 250;
    public static final String QUEUE_NAME = "PendingDecisions";

    private final HzTaskServer taskServer;
    private final int maxPendingWorkers;
    private final int maxPendingLimit;
    private final long sleepOnOverloadMls;

    private final CachedQueue<TaskKey> desTaskQueue;
    private final ExecutorService cachedExecutorService;

    private volatile int size;
    private final Lock lock = new ReentrantLock();

    public PendingDecisionQueueProxy(HazelcastInstance hzInstance, HzTaskServer taskServer, int maxPendingWorkers,
                                     int maxPendingLimit, long sleepOnOverloadMls) {
        this.taskServer = taskServer;
        this.maxPendingWorkers = maxPendingWorkers;
        this.maxPendingLimit = maxPendingLimit;
        this.sleepOnOverloadMls = sleepOnOverloadMls;

        this.desTaskQueue = getQueue(hzInstance);
        this.cachedExecutorService = new ThreadPoolExecutor(3, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
    }


    public boolean stash(TaskKey taskKey) {

        final boolean result = desTaskQueue.offer(taskKey);
        addNewWorkerIfRequired();
        return result;
    }


    private void addNewWorkerIfRequired() {
        if (size < maxPendingWorkers) {
            try {
                if (lock.tryLock(LOCK_TRY_TIME_MLS, TimeUnit.MILLISECONDS)) {
                    try {
                        if (size < maxPendingWorkers && desTaskQueue.size() > 0) {
                            size++;
                            cachedExecutorService.execute(new Worker());
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException ignored) {
                EmptyStatement.ignore(ignored);
            }
        }
    }

    private class Worker implements Runnable {

        @Override
        public void run() {
            try {

                TaskKey taskKey = null;
                do {
                    taskKey = desTaskQueue.poll();
                    if (taskKey == null) {
                        break;
                    }

                    if (taskServer.localExecutorStats.getPendingTaskCount() > maxPendingLimit) {
                        TimeUnit.MILLISECONDS.sleep(sleepOnOverloadMls);
                    }
                    taskServer.sendToClusterMember(taskKey);
                }
                while (true);

                // reduce addNewWorkerIfRequired() pressing
                TimeUnit.SECONDS.sleep(1);

            } catch (InterruptedException ignored) {
                EmptyStatement.ignore(ignored);
            } finally {
                exit();
            }
        }

        void exit() {
            lock.lock();
            try {
                size--;
                addNewWorkerIfRequired();
            } finally {
                lock.unlock();
            }
        }
    }

    private static CachedQueue getQueue(HazelcastInstance hzInstance) {

//        CachedQueueConfig cachedQueueConfig = CachedQueueServiceConfig.getQueueConfig(hzInstance.getConfig(),
//                QUEUE_NAME);
//        cachedQueueConfig.setCacheSize(Integer.MAX_VALUE);


        return CachedQueueServiceConfig.getCachedQueue(hzInstance, QUEUE_NAME);
    }
}
