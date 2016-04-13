package ru.taskurotta.client.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 07.02.13
 * Time: 13:27
 */
public class TaskSpreaderCommon implements TaskSpreader {

    private static final Logger logger = LoggerFactory.getLogger(TaskSpreaderCommon.class);

    private TaskServer taskServer;
    private ActorDefinition actorDefinition;
    private ObjectFactory objectFactory;

    public TaskSpreaderCommon(TaskServer taskServer, ActorDefinition actorDefinition) {
        this.taskServer = taskServer;
        this.actorDefinition = actorDefinition;
        // TODO: receive from constructor args
        this.objectFactory = new ObjectFactory();
    }

    @Override
    public Task poll() {
        logger.trace("Poll for actorDefinition [{}]", actorDefinition);
        TaskContainer taskContainer = taskServer.poll(actorDefinition);
        return objectFactory.parseTask(taskContainer);
    }

    @Override
    public void release(TaskDecision taskDecision) {
        logger.debug("Release decision[{}]", taskDecision);
        DecisionContainer decisionContainer = objectFactory.dumpResult(taskDecision, ActorUtils.getFullActorName(actorDefinition));
        logger.debug("Release decisionContainer[{}]", decisionContainer);
        taskServer.release(decisionContainer);
    }

    @Override
    public void updateTimeout(UUID taskId, UUID processId, long timeout) {
        logger.debug("Update task timeout: taskId[{}], processId[{}], timeout[{}]", taskId, processId, timeout);
        taskServer.updateTaskTimeout(taskId, processId, timeout);
    }

}
