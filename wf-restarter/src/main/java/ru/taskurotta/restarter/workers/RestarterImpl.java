package ru.taskurotta.restarter.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.transport.model.ActorSchedulingOptionsContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.List;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 17:38
 */
public class RestarterImpl implements Restarter {

    private static Logger logger = LoggerFactory.getLogger(RestarterImpl.class);

    private QueueBackend queueBackend;

    @Override
    public void restart(List<TaskContainer> taskContainers) {
        logger.info("Start restarting [{}] task containers", taskContainers);

        for (TaskContainer taskContainer : taskContainers) {

            String taskList = null;
            TaskOptionsContainer taskOptionsContainer = taskContainer.getOptions();
            if (taskOptionsContainer != null) {
                ActorSchedulingOptionsContainer actorSchedulingOptionsContainer = taskOptionsContainer.getActorSchedulingOptions();
                if (actorSchedulingOptionsContainer != null) {
                    taskList = actorSchedulingOptionsContainer.getTaskList();
                }
            }

            logger.debug("Add task container [{}] to queue backend", taskContainer);

            queueBackend.enqueueItem(taskContainer.getActorId(), taskContainer.getTaskId(), taskContainer.getProcessId(), taskContainer.getStartTime(), taskList);
        }

        logger.info("Finish restarting [{}] task containers", taskContainers.size());
    }

    public void setQueueBackend(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
    }
}
