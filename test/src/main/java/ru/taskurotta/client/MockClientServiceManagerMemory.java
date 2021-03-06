package ru.taskurotta.client;

import ru.taskurotta.client.internal.DeciderClientProviderCommon;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.service.MemoryServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.MemoryTaskDao;
import ru.taskurotta.service.storage.TaskService;

public class MockClientServiceManagerMemory implements ClientServiceManager {

    private TaskServer taskServer;
    private MemoryServiceBundle memoryServiceBundle;

    public MockClientServiceManagerMemory() {
        this(60);
    }

    public MockClientServiceManagerMemory(int pollDelay) {
        memoryServiceBundle = new MemoryServiceBundle(pollDelay, new MemoryTaskDao());
        taskServer = new GeneralTaskServer(memoryServiceBundle, 0l);
    }

    @Override
    public DeciderClientProvider getDeciderClientProvider() {
        return new DeciderClientProviderCommon(taskServer);
    }

    @Override
    public TaskSpreaderProvider getTaskSpreaderProvider() {
        return new TaskSpreaderProviderCommon(taskServer);
    }

    public QueueService getQueueService() {
        return memoryServiceBundle.getQueueService();
    }

    public TaskService getTaskService() {
        return memoryServiceBundle.getTaskService();
    }

    public ConfigService getConfigService() {
        return memoryServiceBundle.getConfigService();
    }

}
