package ru.taskurotta.client.internal;

import ru.taskurotta.ProxyFactory;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.core.Task;
import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * User: romario
 * Date: 2/19/13
 * Time: 1:54 PM
 */
public class DeciderClientProviderCommon implements DeciderClientProvider {

    private TaskServer taskServerCommon;
    private ObjectFactory objectFactory;

    public DeciderClientProviderCommon(TaskServer taskServerCommon) {
        this.taskServerCommon = taskServerCommon;
        // TODO: receive from constructor args
        this.objectFactory = new ObjectFactory();

    }


    @Override
    public <DeciderClientType> DeciderClientType getDeciderClient(Class<DeciderClientType> type) {
        return ProxyFactory.getDeciderClient(type, new RuntimeContext(null, null, null) {
            @Override
            public void handle(Task task) {
                startProcess(task);
            }

            /**
             * Always creates new process uuid for new tasks because each DeciderClientType invocation are start of new
             * process.
             *
             * @return
             */
            public UUID getProcessId() {
                return UUID.randomUUID();
            }
        });
    }


    public void startProcess(Task task) {

        TaskContainer taskContainer = objectFactory.dumpTask(task);

        taskServerCommon.startProcess(taskContainer);
    }

}
