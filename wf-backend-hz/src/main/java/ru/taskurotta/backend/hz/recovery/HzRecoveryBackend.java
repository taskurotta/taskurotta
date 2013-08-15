package ru.taskurotta.backend.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.recovery.RecoveryBackend;
import ru.taskurotta.backend.recovery.RecoveryTask;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 15.08.13
 * Time: 15:30
 */
public class HzRecoveryBackend implements RecoveryBackend {

    private static final Logger logger = LoggerFactory.getLogger(HzRecoveryBackend.class);

    private HazelcastInstance hazelcastInstance;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private TaskDao taskDao;
    private ProcessBackend processBackend;

    private int threadCount = 8;

    private String analyzeProcessQueueName = "analyzeProcessQueue";

    private TimeUnit sleepTimeUnit = TimeUnit.SECONDS;
    private long sleepTimeOut = 60;

    public HzRecoveryBackend(HazelcastInstance hazelcastInstance, String analyzeProcessQueueName, QueueBackend queueBackend, DependencyBackend dependencyBackend, TaskDao taskDao, ProcessBackend processBackend) {
        this.hazelcastInstance = hazelcastInstance;
        this.analyzeProcessQueueName = analyzeProcessQueueName;
        this.queueBackend = queueBackend;
        this.dependencyBackend = dependencyBackend;
        this.taskDao = taskDao;
        this.processBackend = processBackend;
    }

    @Override
    public void init() {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        IQueue<UUID> analyzeProcessQueue = hazelcastInstance.getQueue(analyzeProcessQueueName);

        // ToDo (stukushin): may be listener?
        while (true) {
            UUID processId = analyzeProcessQueue.poll();

            if (processId == null) {
                try {
                    sleepTimeUnit.sleep(sleepTimeOut);
                    continue;
                } catch (InterruptedException e) {
                    logger.error("Catch exception while try to sleep", e);
                }
            }

            executorService.submit(new RecoveryTask(queueBackend, dependencyBackend, taskDao, processBackend, processId));
        }
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public void setSleepTimeUnit(TimeUnit sleepTimeUnit) {
        this.sleepTimeUnit = sleepTimeUnit;
    }

    public void setSleepTimeOut(long sleepTimeOut) {
        this.sleepTimeOut = sleepTimeOut;
    }
}
