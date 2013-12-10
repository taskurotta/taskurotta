package ru.taskurotta.backend.hz.gc;

import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
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

        this.garbageCollectorQueue.addItemListener(new ItemListener<UUID>() {
            @Override
            public void itemAdded(final ItemEvent<UUID> item) {
                executorService.submit(new AbstractGCTask(processBackend, graphDao, taskDao) {
                    @Override
                    public void run() {
                        gc(garbageCollectorQueue.poll());
                    }
                });
            }

            @Override
            public void itemRemoved(ItemEvent<UUID> item) {
                // nothing to do
            }
        }, false);
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
