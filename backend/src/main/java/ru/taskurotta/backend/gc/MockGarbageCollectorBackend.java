package ru.taskurotta.backend.gc;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 05.12.13
 * Time: 15:11
 */
public class MockGarbageCollectorBackend implements GarbageCollectorBackend {

    public MockGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao, int poolSize, long delayTime) {}

    public MockGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao, Object queueFactory, String garbageCollectorQueueName, int poolSize, long keepTime) {}

    @Override
    public void delete(UUID processId, String actorId) {
        // do nothing
    }
}
