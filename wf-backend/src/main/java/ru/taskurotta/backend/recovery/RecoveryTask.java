package ru.taskurotta.backend.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.transport.model.ActorSchedulingOptionsContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 15.08.13
 * Time: 14:32
 */
public class RecoveryTask implements Callable {

    private static Logger logger = LoggerFactory.getLogger(RecoveryTask.class);

    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private TaskDao taskDao;
    private ProcessBackend processBackend;

    private UUID processId;

    public RecoveryTask(QueueBackend queueBackend, DependencyBackend dependencyBackend, TaskDao taskDao, ProcessBackend processBackend, UUID processId) {
        this.queueBackend = queueBackend;
        this.dependencyBackend = dependencyBackend;
        this.taskDao = taskDao;
        this.processBackend = processBackend;
        this.processId = processId;
    }

    @Override
    public Object call() throws Exception {
        logger.debug("Try to recovery process [{}]", processId);

        Graph graph = dependencyBackend.getGraph(processId);
        if (graph == null) {
            logger.warn("For processId [{}] not found graph", processId);

            restartProcess(processId);

            return null;
        }

        Collection<TaskContainer> taskContainers = findIncompleteTaskContainers(graph);
        if (taskContainers == null) {
            logger.warn("For processId [{}] not found task containers", processId);

            restartProcess(processId);

            return null;
        }

        restartTasks(taskContainers);

        logger.info("Complete restart process [{}]", processId);

        return null;
    }

    private void restartTasks(Collection<TaskContainer> taskContainers) {
        for (TaskContainer taskContainer : taskContainers) {

            String taskList = null;
            TaskOptionsContainer taskOptionsContainer = taskContainer.getOptions();
            if (taskOptionsContainer != null) {
                ActorSchedulingOptionsContainer actorSchedulingOptionsContainer = taskOptionsContainer.getActorSchedulingOptions();
                if (actorSchedulingOptionsContainer != null) {
                    taskList = actorSchedulingOptionsContainer.getTaskList();
                }
            }

            queueBackend.enqueueItem(taskContainer.getActorId(), taskContainer.getTaskId(), taskContainer.getProcessId(), taskContainer.getStartTime(), taskList);

            logger.debug("Add task container [{}] to queue backend", taskContainer);
        }
    }

    private Collection<TaskContainer> findIncompleteTaskContainers(Graph graph) {
        Map<UUID, Long> notFinishedItems = graph.getNotFinishedItems();
        if (logger.isDebugEnabled()) {
            logger.debug("For processId [{}] found [{}] not finished taskIds", processId, notFinishedItems.size());
        }

        Collection<TaskContainer> taskContainers = new ArrayList<>(notFinishedItems.size());
        Set<UUID> keys = notFinishedItems.keySet();
        for (UUID taskId : keys) {

            TaskContainer taskContainer = taskDao.getTask(taskId, processId);

            if (taskContainer == null) {
                logger.warn("Not found task container [{}] in task repository", taskId);

                return null;
            }

            logger.debug("Found not finished task container [{}]", taskId, taskContainer);
            taskContainers.add(taskContainer);
        }

        if (logger.isInfoEnabled()) {
            logger.info("For processId [{}] found [{}] not finished task containers", processId, taskContainers.size());
        }

        return taskContainers;
    }

    private void restartProcess(UUID processId) {
        TaskContainer startTaskContainer = processBackend.getStartTask(processId);
        logger.info("For processId [{}] get start task [{}]", processId, startTaskContainer);

        // emulate TaskServer.startProcess()
        taskDao.addTask(startTaskContainer);
        dependencyBackend.startProcess(startTaskContainer);

        restartTasks(Arrays.asList(startTaskContainer));

        logger.info("Restart process [{}] from start task [{}]", startTaskContainer.getProcessId(), startTaskContainer);
    }
}
