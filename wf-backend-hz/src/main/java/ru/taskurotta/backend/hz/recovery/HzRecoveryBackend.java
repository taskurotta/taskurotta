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

    private String analyzeProcessQueueName;

    private int threadCount = 8;

    // time out between recovery process in milliseconds
    private long recoveryProcessTimeOut = 600000;

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

        UUID processId = analyzeProcessQueue.poll();
        while (processId != null) {
            executorService.submit(new RecoveryTask(queueBackend, dependencyBackend, taskDao, processBackend, recoveryProcessTimeOut, processId));

            logger.debug("Submit new Recovery task for process [{}]", processId);

            processId = analyzeProcessQueue.poll();
        }

        logger.debug("Queue of analyzing processes is empty");
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public void setRecoveryProcessTimeOut(long recoveryProcessTimeOut) {
        this.recoveryProcessTimeOut = recoveryProcessTimeOut;
    }
}
