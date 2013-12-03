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

    private DelayQueue<DelayFinishedProcess> finishedProcesses = new DelayQueue<>();

    private int poolSize = 8;
    private long initialDelay = 0;
    private long period = 5000;
    private TimeUnit periodTimeUnit = TimeUnit.MILLISECONDS;

    public MemoryGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao) {
        this.configBackend = configBackend;
        this.processBackend = processBackend;
        this.graphDao = graphDao;
        this.taskDao = taskDao;

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(poolSize, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("GC-" + counter++);
                return thread;
            }
        });

        for (int i = 0; i < poolSize; i++) {
            scheduledExecutorService.scheduleAtFixedRate(new GCTask(), initialDelay, period, periodTimeUnit);
        }
    }

    class GCTask implements Runnable {

        @Override
        public void run() {
            DelayFinishedProcess delayFinishedProcess = finishedProcesses.poll();

            if (delayFinishedProcess == null) {
                return;
            }

            UUID processId = delayFinishedProcess.processId;

            logger.trace("Start gc for process [{}]", processId);

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

            logger.debug("Finish gc for process [{}]", processId);
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

        finishedProcesses.add(new DelayFinishedProcess(processId, keepTime));
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public void setSchedule(String schedule) {
        String[] params = schedule.split("\\_");

        if (params.length != 2) {
            logger.warn("Error schedule [{}], use default period [{}] and TimeUnit [{}]", schedule, period, periodTimeUnit);
            return;
        }

        this.period = Long.valueOf(params[0]);
        this.periodTimeUnit = TimeUnit.valueOf(params[1].toUpperCase());
    }
}
