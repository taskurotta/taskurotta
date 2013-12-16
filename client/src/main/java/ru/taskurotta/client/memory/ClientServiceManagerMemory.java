package ru.taskurotta.client.memory;

import ru.taskurotta.service.MemoryServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.MemoryTaskDao;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.client.internal.DeciderClientProviderCommon;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;

/**
 * User: romario
 * Date: 2/20/13
 * Time: 9:50 AM
 */
public class ClientServiceManagerMemory implements ClientServiceManager {

    private TaskServer taskServer;
    private MemoryServiceBundle memoryServiceBundle;

    public ClientServiceManagerMemory() {
        this(60);
    }

    public ClientServiceManagerMemory(int pollDelay) {
        memoryServiceBundle = new MemoryServiceBundle(pollDelay, new MemoryTaskDao());
        taskServer = new GeneralTaskServer(memoryServiceBundle);
    }

    public ClientServiceManagerMemory(TaskServer taskServer) {
        this.taskServer = taskServer;
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
