package ru.taskurotta.restarter.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.restarter.ProcessVO;
import ru.taskurotta.transport.model.ActorSchedulingOptionsContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.model.serialization.JsonSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 17:38
 */
public class RestarterImpl implements Restarter {

    private static Logger logger = LoggerFactory.getLogger(RestarterImpl.class);

    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private TaskDao taskDao;

    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<>(TaskContainer.class);

    @Override
    public void restart(List<ProcessVO> processes) {
        logger.info("Start restarting [{}] processes", processes);

        for (ProcessVO process : processes) {

            List<TaskContainer> taskContainers = findIncompleteTaskContainers(process);

            for (TaskContainer taskContainer : taskContainers) {

                if (taskContainer == null) {
                    continue;
                }

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
        }

        logger.info("Finish restarting [{}] processes", processes.size());
    }

    private List<TaskContainer> findIncompleteTaskContainers(ProcessVO process) {

        UUID processId = process.getId();

        Graph graph = dependencyBackend.getGraph(processId);
        if (graph == null) {
            logger.warn("For processId [{}] not found graph", processId);

            TaskContainer startTaskContainer = taskSerializer.deserialize(process.getStartJson());
            logger.info("For processId [{}] get start task [{}]", processId, startTaskContainer);

            // emulate TaskServer.startProcess()
            taskDao.addTask(startTaskContainer);
            dependencyBackend.startProcess(startTaskContainer);
            logger.info("Restart process [{}] from start task [{}]", processId, startTaskContainer);

            return Arrays.asList(startTaskContainer);
        }

        Set<UUID> notFinishedTaskIds = graph.getNotFinishedItems();
        logger.debug("For processId [{}] found [{}] not finished taskIds", processId, notFinishedTaskIds.size());

        List<TaskContainer> taskContainers = new ArrayList<>(notFinishedTaskIds.size());
        for (UUID taskId : notFinishedTaskIds) {

            TaskContainer taskContainer = taskDao.getTask(taskId, processId);
            if (taskContainer != null) {
                logger.debug("Found not finished task container [{}]", taskId, taskContainer);
                taskContainers.add(taskContainer);
            }
        }

        logger.info("For processId [{}] found [{}] not finished task containers", processId, taskContainers.size());

        return taskContainers;
    }

    public void setQueueBackend(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
    }

    public void setDependencyBackend(DependencyBackend dependencyBackend) {
        this.dependencyBackend = dependencyBackend;
    }

    public void setTaskDao(TaskDao taskDao) {
        this.taskDao = taskDao;
    }
}
