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

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 11:45
 */
public class HzDeciderClientProvider implements DeciderClientProvider {

    private static final Logger logger = LoggerFactory.getLogger(HzDeciderClientProvider.class);

    private HazelcastInstance hazelcastInstance;
    private ObjectFactory objectFactory;

    public HzDeciderClientProvider(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        this.objectFactory = new ObjectFactory();
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
             * @return
             */
            public UUID getProcessId() {
                return UUID.randomUUID();
            }
        });
    }

    @Override
    public void startProcess(Task task) {
        TaskContainer taskContainer = objectFactory.dumpTask(task);

        hazelcastInstance.getExecutorService().submit(new StartProcessTask(taskContainer));
        logger.debug("Create and send distributed task for start process from task [{}]", task);
    }
}
