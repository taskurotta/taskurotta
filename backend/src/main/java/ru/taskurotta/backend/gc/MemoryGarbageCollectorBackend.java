package ru.taskurotta.backend.gc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

public class MemoryGarbageCollectorBackend implements GarbageCollectorBackend {

    private static final Logger logger = LoggerFactory.getLogger(MemoryGarbageCollectorBackend.class);

    private ConfigBackend configBackend;
    private ProcessBackend processBackend;
    private GraphDao graphDao;
    private TaskDao taskDao;

    private DelayQueue<DelayFinishedProcess> garbageCollectorQueue = new DelayQueue<>();

    public MemoryGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao) {
        this(configBackend, processBackend, graphDao, taskDao, 1);
    }

    public MemoryGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao, int poolSize) {
        this.configBackend = configBackend;
        this.processBackend = processBackend;
        this.graphDao = graphDao;
        this.taskDao = taskDao;

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
            executorService.submit(new GCTask());
        }
    }

    class GCTask implements Runnable {

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
                    return;
                }

                UUID processId = delayFinishedProcess.processId;

                logger.trace("Start garbage collector for process [{}]", processId);

                Graph graph = graphDao.getGraph(processId);

                if (graph == null) {
                    logger.error("Not found graph fro process [{}], stop garbage collector for this process", processId);
                    return;
                }

                if (!graph.isFinished()) {
                    logger.error("Graph for process [{}] isn't finished, stop garbage collector for this process", processId);
                    return;
                }

                Set<UUID> finishedItems = graph.getFinishedItems();
                taskDao.deleteDecisions(finishedItems, processId);
                taskDao.deleteTasks(finishedItems, processId);

                graphDao.deleteGraph(processId);

                processBackend.deleteProcess(processId);

                logger.debug("Finish garbage collector for process [{}]", processId);

            }
        }
    }

    class DelayFinishedProcess implements Delayed {

        private UUID processId;
        private long keepTime;

        DelayFinishedProcess(UUID processId, long keepTime) {
            this.processId = processId;
            this.keepTime = keepTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(keepTime, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.valueOf(((DelayFinishedProcess) o).keepTime).compareTo(keepTime);
        }
    }

    @Override
    public void delete(UUID processId, String actorId) {
        ActorPreferences actorPreferences = configBackend.getActorPreferences(actorId);

        long keepTime = 0;
        if (actorPreferences != null) {
            keepTime = actorPreferences.getKeepTime();
        }

        garbageCollectorQueue.add(new DelayFinishedProcess(processId, keepTime));
    }
}
