package ru.taskurotta.backend;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.impl.YamlConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.MemoryDependencyBackend;
import ru.taskurotta.backend.queue.MemoryQueueBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.MemoryProcessBackend;
import ru.taskurotta.backend.storage.MemoryTaskBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:30 PM
 */
public class MemoryBackendBundle implements BackendBundle {

    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;


    public MemoryBackendBundle(int pollDelay) {
        this.processBackend = new MemoryProcessBackend();
        this.taskBackend = new MemoryTaskBackend();
        this.queueBackend = new MemoryQueueBackend(pollDelay);
        this.dependencyBackend = new MemoryDependencyBackend();
        this.configBackend = new YamlConfigBackend();
    }

    @Override
    public ProcessBackend getProcessBackend() {
        return processBackend;
    }

    @Override
    public TaskBackend getTaskBackend() {
        return taskBackend;
    }

    @Override
    public QueueBackend getQueueBackend() {
        return queueBackend;
    }

    @Override
    public DependencyBackend getDependencyBackend() {
        return dependencyBackend;
    }

    @Override
    public ConfigBackend getConfigBackend() {
        return configBackend;
    }
}
