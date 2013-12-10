package ru.taskurotta.backend.hz.gc;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.gc.AbstractGCTask;
import ru.taskurotta.backend.gc.GarbageCollectorBackend;
import ru.taskurotta.backend.hz.queue.delay.DelayIQueue;
import ru.taskurotta.backend.hz.queue.delay.QueueFactory;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class HzGarbageCollectorBackend implements GarbageCollectorBackend {

    private ConfigBackend configBackend;

    private DelayIQueue<UUID> garbageCollectorQueue;

    private long delayTime;

    public HzGarbageCollectorBackend(ConfigBackend configBackend, final ProcessBackend processBackend, final GraphDao graphDao,
                                     final TaskDao taskDao, QueueFactory queueFactory, String garbageCollectorQueueName,
                                     int poolSize, long delayTime) {
        this.configBackend = configBackend;
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
                            processId = garbageCollectorQueue.poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            logger.error("Catch exception while find process for garbage collector", e);
                        }

                        if (processId == null) {
                            continue;
                        }

                        gc(processId);
                    }
                }
            });
        }
    }

    @Override
    public void delete(UUID processId, String actorId) {
        ActorPreferences actorPreferences = configBackend.getActorPreferences(actorId);

        long delayTime = this.delayTime;
        if (actorPreferences != null) {
            delayTime = actorPreferences.getKeepTime();
        }

        garbageCollectorQueue.add(processId, delayTime, TimeUnit.MILLISECONDS);
    }
}
