package ru.taskurotta.client;

import ru.taskurotta.client.internal.DeciderClientProviderCommon;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.hz.HzServiceBundle;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.TaskService;

public class MockClientServiceManager implements ClientServiceManager {

    private TaskServer taskServer;
    private HzServiceBundle serviceBundle;

    public MockClientServiceManager() {
        this(60);
    }

    public MockClientServiceManager(int pollDelay) {
        serviceBundle = new HzServiceBundle(pollDelay);
        taskServer = new GeneralTaskServer(serviceBundle, 0l);
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
        return serviceBundle.getQueueService();
    }

    public TaskService getTaskService() {
        return serviceBundle.getTaskService();
    }

    public ConfigService getConfigService() {
        return serviceBundle.getConfigService();
    }

}
