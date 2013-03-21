package ru.taskurotta.client.memory;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.client.internal.DeciderClientProviderCommon;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.server.TaskDao;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.TaskServerGeneral;
import ru.taskurotta.server.memory.TaskDaoMemory;

/**
 * User: romario
 * Date: 2/20/13
 * Time: 9:50 AM
 */
public class ClientServiceManagerMemory implements ClientServiceManager {

    private TaskServer taskServer;

    public ClientServiceManagerMemory() {
        TaskDao taskDao = new TaskDaoMemory();
        taskServer = new TaskServerGeneral(taskDao);
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
