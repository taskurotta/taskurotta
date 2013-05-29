package ru.taskurotta.backend.ora;

import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;


public class OraBackendBundle implements BackendBundle {


    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;

    public OraBackendBundle(ConfigBackend configBackend, DependencyBackend dependencyBackend, ProcessBackend processBackend, QueueBackend queueBackend, TaskBackend taskBackend) {
        this.configBackend = configBackend;
        this.dependencyBackend = dependencyBackend;
        this.processBackend = processBackend;
        this.queueBackend = queueBackend;
        this.taskBackend = taskBackend;
    }

    public ConfigBackend getConfigBackend() {
        return configBackend;
    }

    public DependencyBackend getDependencyBackend() {
        return dependencyBackend;
    }

    public ProcessBackend getProcessBackend() {
        return processBackend;
    }

    public QueueBackend getQueueBackend() {
        return queueBackend;
    }

    public TaskBackend getTaskBackend() {
        return taskBackend;
    }
}
