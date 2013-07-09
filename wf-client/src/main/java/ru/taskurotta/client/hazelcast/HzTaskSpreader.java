package ru.taskurotta.client.hazelcast;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.ExecutionCallback;
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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 11:54
 */
public class HzTaskSpreader implements TaskSpreader {

    private static final Logger logger = LoggerFactory.getLogger(HzTaskSpreader.class);

    private HazelcastInstance hazelcastInstance;
    private ActorDefinition actorDefinition;
    private ObjectFactory objectFactory;

    private volatile ConcurrentLinkedQueue<TaskContainer> queue = new ConcurrentLinkedQueue<>();

    public HzTaskSpreader(HazelcastInstance hazelcastInstance, ActorDefinition actorDefinition) {
        this.hazelcastInstance = hazelcastInstance;
        this.actorDefinition = actorDefinition;
        this.objectFactory = new ObjectFactory();
    }

    @Override
    public Task poll() {
        logger.debug("Poll");

        DistributedTask<TaskContainer> distributedTask = new DistributedTask<>(new PollTask(actorDefinition));

        distributedTask.setExecutionCallback(new ExecutionCallback<TaskContainer>() {
            @Override
            public void done(Future<TaskContainer> taskContainerFuture) {
                try {
                    logger.debug("Poll task container future [{}]", taskContainerFuture);

                    TaskContainer taskContainer = taskContainerFuture.get();

                    logger.debug("Poll task container [{}]", taskContainer);

                    queue.add(taskContainer);

                    logger.debug("Queue size = [{}]", queue.size());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });

        hazelcastInstance.getExecutorService().submit(distributedTask);

        TaskContainer taskContainer = queue.poll();

        logger.debug("Before return queue task container [{}]", taskContainer);

        return objectFactory.parseTask(taskContainer);
    }

    @Override
    public void release(TaskDecision taskDecision) {

        logger.debug("Try to release task decision [{}]", taskDecision);

        DecisionContainer decisionContainer = objectFactory.dumpResult(taskDecision);

        hazelcastInstance.getExecutorService().submit(new DistributedTask<>(new ReleaseTask(decisionContainer)));
    }
}
