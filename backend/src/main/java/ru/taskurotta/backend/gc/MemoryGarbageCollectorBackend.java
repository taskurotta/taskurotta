package ru.taskurotta.backend.gc;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;

import java.util.UUID;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MemoryGarbageCollectorBackend implements GarbageCollectorBackend {

    private ConfigBackend configBackend;

    private DelayQueue<DelayFinishedProcess> garbageCollectorQueue = new DelayQueue<>();

    private long delayTime;

    public MemoryGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao,
                                         TaskDao taskDao, int poolSize, long delayTime) {
        this.configBackend = configBackend;
        this.delayTime = delayTime;

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
            executorService.submit(new MemoryGCTask(processBackend, graphDao, taskDao));
        }
    }

    class DelayFinishedProcess implements Delayed {

        private UUID processId;
        private long enqueueTime;

        DelayFinishedProcess(UUID processId, long enqueueTime) {
            this.processId = processId;
            this.enqueueTime = enqueueTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(enqueueTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.valueOf(((DelayFinishedProcess) o).enqueueTime).compareTo(enqueueTime);
        }
    }

    class MemoryGCTask extends AbstractGCTask {

        protected MemoryGCTask(ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao) {
            super(processBackend, graphDao, taskDao);
        }

        @Override
        public void run() {

            while(true) {

                logger.trace("Try to get process for garbage collector");

                DelayFinishedProcess delayFinishedProcess = null;
                try {
                    delayFinishedProcess = garbageCollectorQueue.take();
                } catch (InterruptedException e) {
                    logger.error("Catch exception while find process for garbage collector", e);
                }

                if (delayFinishedProcess == null) {
                    continue;
                }

                gc(delayFinishedProcess.processId);
            }
        }
    }

    @Override
    public void delete(UUID processId, String actorId) {
        ActorPreferences actorPreferences = configBackend.getActorPreferences(actorId);

        long delayTime = this.delayTime;
        if (actorPreferences != null) {
            delayTime = actorPreferences.getKeepTime();
        }

        garbageCollectorQueue.add(new DelayFinishedProcess(processId, System.currentTimeMillis() + delayTime));
    }

    public void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
    }
}
