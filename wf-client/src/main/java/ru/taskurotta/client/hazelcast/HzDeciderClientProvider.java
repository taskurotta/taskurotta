package ru.taskurotta.client.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.ProxyFactory;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.hazelcast.task.StartProcessTask;
import ru.taskurotta.core.Task;
import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 11:45
 */
public class HzDeciderClientProvider implements DeciderClientProvider {

    private static final Logger logger = LoggerFactory.getLogger(HzDeciderClientProvider.class);

    private ObjectFactory objectFactory;

    private ExecutorService executorService;

    public HzDeciderClientProvider(HazelcastInstance hazelcastInstance) {

        this.objectFactory = new ObjectFactory();

        this.executorService = hazelcastInstance.getExecutorService("startProcessExecutorService");
    }

    @Override
    public <DeciderClientType> DeciderClientType getDeciderClient(Class<DeciderClientType> type) {
        return ProxyFactory.getDeciderClient(type, new RuntimeContext(null) {
            @Override
            public void handle(Task task) {
                startProcess(task);
            }

            /**
             * Always creates new process uuid for new tasks because each DeciderClientType invocation are start of new
             * process.
             *
             * @return process UUID
             */
            public UUID getProcessId() {
                return UUID.randomUUID();
            }
        });
    }

    @Override
    public void startProcess(Task task) {
        logger.trace("Try to start process from task [{}]", task);

        TaskContainer taskContainer = objectFactory.dumpTask(task);
        executorService.submit(new StartProcessTask(taskContainer));

        logger.debug("Create and send distributed task for start process from task [{}]", task);
    }
}
