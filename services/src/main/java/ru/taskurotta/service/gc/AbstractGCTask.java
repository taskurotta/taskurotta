package ru.taskurotta.service.gc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskDao;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 05.12.13
 * Time: 12:41
 */
public abstract class AbstractGCTask implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractGCTask.class);

    protected ProcessService processService;
    protected GraphDao graphDao;
    protected TaskDao taskDao;

    protected AbstractGCTask(ProcessService processService, GraphDao graphDao, TaskDao taskDao) {
        this.processService = processService;
        this.graphDao = graphDao;
        this.taskDao = taskDao;
    }

    protected void gc(UUID processId) {
        logger.trace("Start garbage collector for process [{}]", processId);

        if (processId == null) {
            logger.warn("ProcessId for garbage collector is null");
            return;
        }

        Process process = processService.getProcess(processId);
        if (process == null) {
            logger.warn("Not found process [{}]", processId);
            return;
        }
        boolean isAborted = process.getState() == Process.ABORTED;

        Graph graph = graphDao.getGraph(processId);
        if (graph == null) {
            if (!isAborted) {
                logger.warn("Not found graph for process [{}], stop garbage collector for this process", processId);
                if (processService.getStartTask(processId) == null) {
                    logger.warn("And processService has no start task for it [{}]", processId);
                }
                return;
            }
        } else {
            if (!graph.isFinished() && !isAborted) {
                logger.error("Graph for process [{}] isn't finished, stop garbage collector for this process", processId);
                return;
            }

            Set<UUID> finishedItems = graph.getFinishedItems();
            deleteTasksAnsDecisions(finishedItems, processId);

            Set<UUID> notFinishedItems = graph.getNotFinishedItems().keySet();
            deleteTasksAnsDecisions(notFinishedItems, processId);

            UUID[] readyItems = graph.getReadyItems();
            if (readyItems != null) {
                Set<UUID> rItems = new HashSet<>(Arrays.asList(readyItems));
                deleteTasksAnsDecisions(rItems, processId);
            }

            graphDao.deleteGraph(processId);
        }

        processService.deleteProcess(processId);

        logger.debug("Finish garbage collector for process [{}]", processId);
    }

    private void deleteTasksAnsDecisions(Set<UUID> taskIds, UUID processId) {
        taskDao.deleteDecisions(taskIds, processId);
        taskDao.deleteTasks(taskIds, processId);
    }
}