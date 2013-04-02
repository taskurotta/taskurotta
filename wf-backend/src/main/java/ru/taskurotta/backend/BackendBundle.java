package ru.taskurotta.backend;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.StorageBackend;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:28 PM
 */
public interface BackendBundle {

    public StorageBackend getStorageBackend();

    public QueueBackend getQueueBackend();

    public DependencyBackend getDependencyBackend();

    public ConfigBackend getConfigBackend();

}
