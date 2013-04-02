package ru.taskurotta.backend;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.MemoryDependencyBackend;
import ru.taskurotta.backend.queue.MemoryQueueBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.MemoryStorageBackend;
import ru.taskurotta.backend.storage.StorageBackend;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:30 PM
 */
public class MemoryBackendBundle implements BackendBundle {

    private StorageBackend storageBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;


    public MemoryBackendBundle(int pollDelay) {
        this.storageBackend = new MemoryStorageBackend();
        this.queueBackend = new MemoryQueueBackend(pollDelay);
        this.dependencyBackend = new MemoryDependencyBackend();
        this.configBackend = new ConfigBackend();
    }

    @Override
    public StorageBackend getStorageBackend() {
        return storageBackend;
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
