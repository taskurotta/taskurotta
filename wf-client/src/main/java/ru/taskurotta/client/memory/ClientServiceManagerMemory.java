package ru.taskurotta.client.memory;

import ru.taskurotta.backend.MemoryBackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.MemoryTaskDao;
import ru.taskurotta.backend.storage.TaskBackend;
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
    private MemoryBackendBundle memoryBackendBundle;

    public ClientServiceManagerMemory() {
        memoryBackendBundle = new MemoryBackendBundle(60, new MemoryTaskDao());
        taskServer = new GeneralTaskServer(memoryBackendBundle);
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

    public QueueBackend getQueueBackend() {
        return memoryBackendBundle.getQueueBackend();
    }

    public TaskBackend getTaskBackend() {
        return memoryBackendBundle.getTaskBackend();
    }

    public ConfigBackend getConfigBackend() {
        return memoryBackendBundle.getConfigBackend();
    }


}
