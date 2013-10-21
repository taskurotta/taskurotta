package ru.taskurotta.backend;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:28 PM
 */
public interface BackendBundle {

    public ProcessBackend getProcessBackend();

    public TaskBackend getTaskBackend();

    public QueueBackend getQueueBackend();

    public DependencyBackend getDependencyBackend();

    public ConfigBackend getConfigBackend();

    public BrokenProcessBackend getBrokenProcessBackend();
}
