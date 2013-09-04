package ru.taskurotta.client.internal;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.server.TaskServer;

/**
 * ClientServiceManager common implementation based on passed TaskServer instance
 * User: dimadin
 * Date: 24.04.13
 * Time: 11:24
 */
public class CommonClientServiceManager implements ClientServiceManager {

    private TaskServer taskServer;

    public CommonClientServiceManager(TaskServer taskServer) {
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
