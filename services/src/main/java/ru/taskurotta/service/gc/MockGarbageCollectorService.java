package ru.taskurotta.service.gc;

import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskDao;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 05.12.13
 * Time: 15:11
 */
public class MockGarbageCollectorService implements GarbageCollectorService {

    public MockGarbageCollectorService(ConfigService configService, ProcessService processService, GraphDao graphDao, TaskDao taskDao, int poolSize, long delayTime) {}

    public MockGarbageCollectorService(ConfigService configService, ProcessService processService, GraphDao graphDao, TaskDao taskDao, Object queueFactory, String garbageCollectorQueueName, int poolSize, long delayTime) {}

    @Override
    public void delete(UUID processId) {
        // do nothing
    }

    @Override
    public int getCurrentSize() {
        return 0;
    }
}
