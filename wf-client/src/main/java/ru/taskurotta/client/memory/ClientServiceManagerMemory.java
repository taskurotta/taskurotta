package ru.taskurotta.client.memory;

import ru.taskurotta.backend.MemoryBackendBundle;
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

    public ClientServiceManagerMemory() {
        MemoryBackendBundle memoryBackendBundle = new MemoryBackendBundle(60);
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
}
