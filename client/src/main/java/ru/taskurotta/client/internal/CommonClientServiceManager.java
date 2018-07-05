package ru.taskurotta.client.internal;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;

/**
 * ClientServiceManager common implementation based on passed TaskServer instance
 * User: dimadin
 * Date: 24.04.13
 * Time: 11:24
 */
public class CommonClientServiceManager implements ClientServiceManager {

    private TaskServer taskServer;
    private ObjectFactory objectFactory;

    public CommonClientServiceManager(TaskServer taskServer, ObjectFactory objectFactory) {
        this.taskServer = taskServer;
        this.objectFactory = objectFactory;
    }

    @Override
    public DeciderClientProvider getDeciderClientProvider() {
        return new DeciderClientProviderCommon(taskServer);
    }

    @Override
    public TaskSpreaderProvider getTaskSpreaderProvider() {
        return new TaskSpreaderProviderCommon(taskServer, objectFactory);
    }

}
