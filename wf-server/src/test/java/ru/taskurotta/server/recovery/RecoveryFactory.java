package ru.taskurotta.server.recovery;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.MemoryBackendBundle;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.impl.MemoryConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.MemoryTaskDao;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.server.GeneralTaskServer;

public class RecoveryFactory {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryFactory.class);

    public static final int timeout = 100;
    public static final TimeUnit timeoutUnit = TimeUnit.MILLISECONDS;

    private static MemoryBackendBundle backends;
    private static GeneralTaskServer taskServer;

    static {
        backends = new MemoryBackendBundle(1, new MemoryTaskDao());
        ConfigBackend config = backends.getConfigBackend();
        if(config instanceof MemoryConfigBackend) {
            ((MemoryConfigBackend) config).setDefaultTimeout(timeout);
            ((MemoryConfigBackend) config).setDefaultTimeunit(timeoutUnit);
        }
        taskServer = new GeneralTaskServer(backends);

    }


    public static GeneralTaskServer getTaskServer() {
        return taskServer;
    }

    public static QueueBackend getQueueBackend() {
        return backends.getQueueBackend();
    }

    public static CheckpointService getCheckpointService(Class<?> backendClazz) {
        if(QueueBackend.class.isAssignableFrom(backendClazz)) {
            return getQueueBackend().getCheckpointService();
        } else if(TaskBackend.class.isAssignableFrom(backendClazz)) {
            return getTaskBackend().getCheckpointService();
        } else if(ProcessBackend.class.isAssignableFrom(backendClazz)) {
            return getProcessBackend().getCheckpointService();
        }
        return null;
    }

    public static ConfigBackend getConfigBackend() {
        return backends.getConfigBackend();
    }

    public static ProcessBackend getProcessBackend() {
        return backends.getProcessBackend();
    }

    public static DependencyBackend getDependencyBackend() {
        return backends.getDependencyBackend();
    }

    public static TaskBackend getTaskBackend() {
        return backends.getTaskBackend();
    }

    public static MemoryBackendBundle getMemoryBackendBundle() {
        return backends;
    }

    public static TaskContainer getWorkerTaskContainer(UUID taskId, UUID processId) {
        return new TaskContainer(taskId, processId, "testMethod1", "testWorker#0.1", TaskType.WORKER, System.currentTimeMillis(), 5, null, null);
    }

    public static TaskContainer getDeciderTaskContainer(UUID taskId, UUID processId) {
        return new TaskContainer(taskId, processId, "testMethod2", "testDecider#0.1", TaskType.DECIDER_START, System.currentTimeMillis(), 5, null, null);
    }

    public static TaskBackendEnqueueTaskRecovery getTaskRecoveryProcess(TimeoutType timeoutType) {

        TaskBackendEnqueueTaskRecovery recovery = new TaskBackendEnqueueTaskRecovery();
        recovery.setConfigBackend(RecoveryFactory.getConfigBackend());
        recovery.setTaskBackend(RecoveryFactory.getTaskBackend());
        recovery.setQueueBackend(RecoveryFactory.getQueueBackend());
        recovery.setRecoveryPeriod(10);
        recovery.setRecoveryPeriodUnit(TimeUnit.MINUTES);
        recovery.setTimeIterationStep(500);
        recovery.setTimeIterationStepUnit(TimeUnit.SECONDS);
        recovery.setTimeoutType(timeoutType);

        return recovery;
    }

    public static QueueBackendEnqueueTaskRecovery getQueueRecoveryProcess(TimeoutType timeoutType) {

        QueueBackendEnqueueTaskRecovery recovery = new QueueBackendEnqueueTaskRecovery();
        recovery.setConfigBackend(RecoveryFactory.getConfigBackend());
        recovery.setTaskBackend(RecoveryFactory.getTaskBackend());
        recovery.setQueueBackend(RecoveryFactory.getQueueBackend());
        recovery.setRecoveryPeriod(10);
        recovery.setRecoveryPeriodUnit(TimeUnit.MINUTES);
        recovery.setTimeIterationStep(500);
        recovery.setTimeIterationStepUnit(TimeUnit.SECONDS);
        recovery.setTimeoutType(timeoutType);

        return recovery;
    }

    public static ProcessBackendEnqueueTaskRecovery getProcessRecoveryProcess(TimeoutType timeoutType) {

        ProcessBackendEnqueueTaskRecovery recovery = new ProcessBackendEnqueueTaskRecovery();
        recovery.setConfigBackend(RecoveryFactory.getConfigBackend());
        recovery.setProcessBackend(RecoveryFactory.getProcessBackend());
        recovery.setTaskBackend(RecoveryFactory.getTaskBackend());
        recovery.setQueueBackend(RecoveryFactory.getQueueBackend());
        recovery.setRecoveryPeriod(10);
        recovery.setRecoveryPeriodUnit(TimeUnit.MINUTES);
        recovery.setTimeIterationStep(500);
        recovery.setTimeIterationStepUnit(TimeUnit.SECONDS);
        recovery.setTimeoutType(timeoutType);

        return recovery;
    }

    public static void ensureExpiration() {
        long sleepFor = timeoutUnit.toMillis(timeout);
        try {
            Thread.sleep(sleepFor);
        } catch (InterruptedException e) {
            logger.error("Thread sleep for["+sleepFor+"] interrupted!", e);
        }
    }


}
