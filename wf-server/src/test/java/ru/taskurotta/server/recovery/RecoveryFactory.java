package ru.taskurotta.server.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.MemoryBackendBundle;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.impl.MemoryConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.MemoryTaskDao;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.server.GeneralTaskServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Creates and stores entities for test needs
 */
public class RecoveryFactory {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryFactory.class);

    public int timeout = 100;
    public TimeUnit timeoutUnit = TimeUnit.MILLISECONDS;

    private MemoryBackendBundle backends;
    private GeneralTaskServer taskServer;

    public RecoveryFactory(int timeout, TimeUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        backends = new MemoryBackendBundle(1, new MemoryTaskDao());
        ConfigBackend config = backends.getConfigBackend();
        if (config instanceof MemoryConfigBackend) {
            ((MemoryConfigBackend) config).setDefaultTimeout(timeout);
            ((MemoryConfigBackend) config).setDefaultTimeunit(timeoutUnit);
        }
        taskServer = new GeneralTaskServer(backends);
    }

    public GeneralTaskServer getTaskServer() {
        return taskServer;
    }

    public QueueBackend getQueueBackend() {
        return backends.getQueueBackend();
    }

    public CheckpointService getCheckpointService(Class<?> backendClazz) {
        if (QueueBackend.class.isAssignableFrom(backendClazz)) {
            return getQueueBackend().getCheckpointService();
        } else if (TaskBackend.class.isAssignableFrom(backendClazz)) {
            return getTaskBackend().getCheckpointService();
        } else if (ProcessBackend.class.isAssignableFrom(backendClazz)) {
            return getProcessBackend().getCheckpointService();
        }
        return null;
    }

    public ConfigBackend getConfigBackend() {
        return backends.getConfigBackend();
    }

    public ProcessBackend getProcessBackend() {
        return backends.getProcessBackend();
    }

    public DependencyBackend getDependencyBackend() {
        return backends.getDependencyBackend();
    }

    public TaskBackend getTaskBackend() {
        return backends.getTaskBackend();
    }

    public MemoryBackendBundle getMemoryBackendBundle() {
        return backends;
    }

    public RetryEnqueueRecovery getRecoveryProcess(Class<?> backendClazz) {
        RetryEnqueueRecovery result = new RetryEnqueueRecovery();
        result.setConfigBackend(backends.getConfigBackend());
        result.setQueueBackend(backends.getQueueBackend());
        result.setRecoveryPeriod(100);
        result.setRecoveryPeriodUnit(TimeUnit.SECONDS);
        result.setTaskBackend(backends.getTaskBackend());
        result.setTimeIterationStep(500);
        result.setTimeIterationStepUnit(TimeUnit.MILLISECONDS);

        List<CheckpointService> checkpointServices = new ArrayList<CheckpointService>();
        checkpointServices.add(getCheckpointService(backendClazz));
        result.setCheckpointServices(checkpointServices);

        return result;
    }

}
