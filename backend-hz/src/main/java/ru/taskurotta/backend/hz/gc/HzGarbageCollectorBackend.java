package ru.taskurotta.backend.hz.gc;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.gc.AbstractGCTask;
import ru.taskurotta.backend.gc.GarbageCollectorBackend;
import ru.taskurotta.backend.hz.queue.delay.BaseQueueFactory;
import ru.taskurotta.backend.hz.queue.delay.BaseStorageFactory;
import ru.taskurotta.backend.hz.queue.delay.DelayIQueue;
import ru.taskurotta.backend.hz.queue.delay.QueueFactory;
import ru.taskurotta.backend.hz.queue.delay.StorageFactory;
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

    public HzGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao, HazelcastInstance hazelcastInstance) {
        this(configBackend, processBackend, graphDao, taskDao, hazelcastInstance, "garbageCollectorQueue", 1);
    }

    public HzGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao, HazelcastInstance hazelcastInstance, String garbageCollectorQueueName) {
        this(configBackend, processBackend, graphDao, taskDao, hazelcastInstance, garbageCollectorQueueName, 1);
    }

    public HzGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao, HazelcastInstance hazelcastInstance, String garbageCollectorQueueName, int poolSize) {
        this.configBackend = configBackend;

        StorageFactory storageFactory = new BaseStorageFactory(hazelcastInstance, "dqs#");
        QueueFactory queueFactory = new BaseQueueFactory(hazelcastInstance, storageFactory);
        this.garbageCollectorQueue = queueFactory.create(garbageCollectorQueueName);

        ExecutorService executorService = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("GC-" + counter++);
                return thread;
            }
        });

        for (int i = 0; i < poolSize; i++) {
            executorService.submit(new HazelcastGCTask(processBackend, graphDao, taskDao));
        }
    }

    @Override
    public void delete(UUID processId, String actorId) {
        ActorPreferences actorPreferences = configBackend.getActorPreferences(actorId);

        long keepTime = 0;
        if (actorPreferences != null) {
            keepTime = actorPreferences.getKeepTime();
        }

        garbageCollectorQueue.add(processId, keepTime, TimeUnit.MILLISECONDS);
    }

    class HazelcastGCTask extends AbstractGCTask {

        protected HazelcastGCTask(ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao) {
            super(processBackend, graphDao, taskDao);
        }

        @Override
        public void run() {
            while(true) {

                logger.trace("Try to get process for garbage collector");

                UUID processId = null;
                try {
                    processId = garbageCollectorQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("Catch exception while find process for garbage collector", e);
                }

                if (processId == null) {
                    continue;
                }

                gc(processId);
            }
        }
    }
}
