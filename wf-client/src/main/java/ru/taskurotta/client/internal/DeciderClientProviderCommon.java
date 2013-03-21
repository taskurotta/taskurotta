package ru.taskurotta.client.internal;

import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.RuntimeProviderManager;
import ru.taskurotta.TaskHandler;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.core.Task;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.server.transport.TaskContainer;

/**
 * User: romario
 * Date: 2/19/13
 * Time: 1:54 PM
 */
public class DeciderClientProviderCommon implements DeciderClientProvider {

    private RuntimeProvider runtimeProvider;
    private TaskServer taskServerCommon;
    private ObjectFactory objectFactory;

    public DeciderClientProviderCommon(TaskServer taskServerCommon) {
        this.taskServerCommon = taskServerCommon;
        this.runtimeProvider = RuntimeProviderManager.getRuntimeProvider(new TaskHandler() {
            @Override
            public void handle(Task task) {
                startProcess(task);
            }
        });
        // TODO: receive from constructor args
        this.objectFactory = new ObjectFactory();

    }


    @Override
    public <DeciderClientType> DeciderClientType getDeciderClient(Class<DeciderClientType> type) {
        return runtimeProvider.getDeciderClient(type);
    }


    public void startProcess(Task task) {

        TaskContainer taskContainer = objectFactory.dumpTask(task);

        taskServerCommon.startProcess(taskContainer);
    }

}
