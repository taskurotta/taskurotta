package ru.taskurotta.client.hazelcast;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 11:54
 */
public class HzTaskSpreader implements TaskSpreader {

    private static final Logger logger = LoggerFactory.getLogger(HzTaskSpreader.class);

    private ActorDefinition actorDefinition;
    private ObjectFactory objectFactory;

    private ExecutorService executorService;

    public HzTaskSpreader(HazelcastInstance hazelcastInstance, ActorDefinition actorDefinition) {
        this.actorDefinition = actorDefinition;
        this.objectFactory = new ObjectFactory();

        this.executorService = hazelcastInstance.getExecutorService("pollReleaseExecutorService");
    }

    @Override
    public Task poll() {
        logger.trace("Try poll for actor definition [{}]", actorDefinition);

        Future<?> future = executorService.submit(new DistributedTask<>(new PollTask(actorDefinition)));
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

        DecisionContainer decisionContainer = objectFactory.dumpResult(taskDecision);

        executorService.submit(new DistributedTask<>(new ReleaseTask(decisionContainer), taskDecision.getProcessId()));

        logger.debug("Create and send distributed task for release decision container [{}]", decisionContainer);
    }

}
