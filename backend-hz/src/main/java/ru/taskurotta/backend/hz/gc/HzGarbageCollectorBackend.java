package ru.taskurotta.backend.hz.gc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.gc.AbstractGCTask;
import ru.taskurotta.backend.gc.GarbageCollectorBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.hazelcast.queue.delay.DelayIQueue;
import ru.taskurotta.hazelcast.queue.delay.QueueFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class HzGarbageCollectorBackend implements GarbageCollectorBackend {

    private static final Logger logger = LoggerFactory.getLogger(HzGarbageCollectorBackend.class);

    private long delayTime;
    private boolean enabled;

    private DelayIQueue<UUID> garbageCollectorQueue;

    public HzGarbageCollectorBackend(final ProcessBackend processBackend, final GraphDao graphDao,
                                     final TaskDao taskDao, QueueFactory queueFactory, String garbageCollectorQueueName,
                                     int poolSize, long delayTime, boolean enabled) {

        logger.debug("Garbage Collector initialization {}", enabled);

        this.enabled = enabled;

        if (!enabled) {
            return;
        }

        this.delayTime = delayTime;

        this.garbageCollectorQueue = queueFactory.create(garbageCollectorQueueName);

        final ExecutorService executorService = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("GC-" + counter++);
                return thread;
            }
        });

        for (int i = 0; i < poolSize; i++) {
            executorService.submit(new AbstractGCTask(processBackend, graphDao, taskDao) {
                @Override
                public void run() {
                    while (true) {
                        logger.trace("Try to get process for garbage collector");

                        UUID processId = null;
                        try {
                            processId = garbageCollectorQueue.take();
                        } catch (InterruptedException e) {
                            logger.error("Catch exception while find process for garbage collector", e);
                        }

                        gc(processId);
                    }
                }
            });
        }
    }

    @Override
    public void delete(UUID processId) {

        if (!enabled) {
            return;
        }

        garbageCollectorQueue.add(processId, delayTime, TimeUnit.MILLISECONDS);

    }
}
