package ru.taskurotta.service.gc;

import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskDao;

import java.util.UUID;
import java.util.concurrent.*;

public class MemoryGarbageCollectorService implements GarbageCollectorService {

    private DelayQueue<DelayFinishedProcess> garbageCollectorQueue = new DelayQueue<>();

    public MemoryGarbageCollectorService(ProcessService processService, GraphDao graphDao,
                                         TaskDao taskDao, int poolSize) {

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(poolSize, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("GC-" + counter++);
                thread.setDaemon(true);
                return thread;
            }
        });

        for (int i = 0; i < poolSize; i++) {
            executorService.scheduleWithFixedDelay(new AbstractGCTask(processService, graphDao, taskDao) {
                @Override
                public void run() {
                    while (!garbageCollectorQueue.isEmpty()) {
                        DelayFinishedProcess delayFinishedProcess = null;
                        try {
                            delayFinishedProcess = garbageCollectorQueue.take();
                        } catch (InterruptedException e) {
                            logger.error("Catch exception while find process for garbage collector", e);
                        }

                        if (delayFinishedProcess != null) {
                            gc(delayFinishedProcess.processId);
                        }
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
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
    public void collect(UUID processId, long timeout) {
        garbageCollectorQueue.add(new DelayFinishedProcess(processId, System.currentTimeMillis() + timeout));
    }

    @Override
    public int getCurrentSize() {
        return garbageCollectorQueue.size();
    }
}
