package ru.taskurotta.client.hazelcast.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 12:24
 */
public class StartProcessTask implements Callable<Void>, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(StartProcessTask.class);

    private TaskContainer taskContainer;

    public StartProcessTask(TaskContainer taskContainer) {
        this.taskContainer = taskContainer;
    }

    @Override
    public Void call() throws Exception {
        logger.trace("Try to start process from task [{}]", taskContainer);

        HazelcastTaskServer taskServer = HazelcastTaskServer.getInstance();

        logger.trace("Get HazelcastTaskServer instance [{}]", taskServer);

        taskServer.startProcess(taskContainer);

        logger.debug("Start process on taskServer [{}] from task [{}]", taskServer, taskContainer);

        return null;
    }
}