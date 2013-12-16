package ru.taskurotta.service.gc;

import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskDao;

import java.util.UUID;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MemoryGarbageCollectorService implements GarbageCollectorService {

    private DelayQueue<DelayFinishedProcess> garbageCollectorQueue = new DelayQueue<>();

    private long delayTime;

    public MemoryGarbageCollectorService(ProcessService processService, GraphDao graphDao,
                                         TaskDao taskDao, int poolSize, long timeBeforeDelete) {
        this.delayTime = timeBeforeDelete;

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
            executorService.submit(new AbstractGCTask(processService, graphDao, taskDao) {
                @Override
                public void run() {
                    while(true) {
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
            });
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

    @Override
    public void delete(UUID processId) {
        garbageCollectorQueue.add(new DelayFinishedProcess(processId, System.currentTimeMillis() + delayTime));
    }
}
