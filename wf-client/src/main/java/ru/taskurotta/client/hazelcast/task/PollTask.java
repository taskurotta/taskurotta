package ru.taskurotta.client.hazelcast.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 12:35
 */
public class PollTask implements Callable<TaskContainer>, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(PollTask.class);

    private ActorDefinition actorDefinition;

    public PollTask(ActorDefinition actorDefinition) {
        this.actorDefinition = actorDefinition;
    }

    @Override
    public TaskContainer call() throws Exception {
        logger.debug("Try to poll task container for actor definition [{}]", actorDefinition);

        TaskContainer taskContainer = HazelcastTaskServer.getInstance().poll(actorDefinition);

        logger.debug("Successfully poll task container [{}] for actor definition [{}]", taskContainer, actorDefinition);

        return taskContainer;
    }
}
