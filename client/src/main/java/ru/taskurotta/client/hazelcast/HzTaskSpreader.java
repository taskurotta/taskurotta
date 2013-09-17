package ru.taskurotta.client.hazelcast;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.hazelcast.task.PollTask;
import ru.taskurotta.client.hazelcast.task.ReleaseTask;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 11:54
 */
public class HzTaskSpreader implements TaskSpreader {

    private static final Logger logger = LoggerFactory.getLogger(HzTaskSpreader.class);

    private ActorDefinition actorDefinition;
    private ObjectFactory objectFactory;

    private IExecutorService pollExecutorService;
    private IExecutorService releaseExecutorService;

    public HzTaskSpreader(HazelcastInstance hazelcastInstance, ActorDefinition actorDefinition) {
        this.actorDefinition = actorDefinition;
        this.objectFactory = new ObjectFactory();

        this.pollExecutorService = hazelcastInstance.getExecutorService("pollExecutorService");
        this.releaseExecutorService = hazelcastInstance.getExecutorService("releaseExecutorService");
    }

    @Override
    public Task poll() {
        logger.trace("Try poll for actor definition [{}]", actorDefinition);

        Future<?> future = pollExecutorService.submit(new PollTask(actorDefinition));
        TaskContainer taskContainer;
        try {
            taskContainer = (TaskContainer) future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Catch exception while poll task for actor definition [" + actorDefinition + "]", e);
            return null;
        }

        logger.debug("Before poll return task container [{}] for actor definition [{}]", taskContainer, actorDefinition);

        return objectFactory.parseTask(taskContainer);
    }

    @Override
    public void release(TaskDecision taskDecision) {
        logger.trace("Try to release task decision [{}]", taskDecision);

        DecisionContainer decisionContainer = objectFactory.dumpResult(taskDecision, actorDefinition.getFullName());

        Future<?> future = releaseExecutorService.submitToKeyOwner(new ReleaseTask(decisionContainer), taskDecision.getProcessId());
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Catch exception while release decision container [" + taskDecision + "]", e);
        }

        logger.debug("Release decision container [{}]", decisionContainer);
    }

}
