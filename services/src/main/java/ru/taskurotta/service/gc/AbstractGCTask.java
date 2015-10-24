package ru.taskurotta.service.gc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskDao;

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

        Graph graph = graphDao.getGraph(processId);

        if (graph != null && !graph.isFinished()) {
            logger.error("Graph for process [{}] isn't finished, stop garbage collector for this process", processId);
            return;
        }

        if (graph == null) {
            logger.warn("Not found graph for process [{}], stop garbage collector for this process", processId);
            if (processService.getStartTask(processId) == null) {
                logger.warn("And processService has no start task for it [{}]", processId);
            }
        }  else {

            Set<UUID> finishedItems = graph.getFinishedItems();
            taskDao.deleteDecisions(finishedItems, processId);
            taskDao.deleteTasks(finishedItems, processId);

            // todo: remove unfinished and interrupted tasks

            graphDao.deleteGraph(processId);

        }

        if (process != null) {
            processService.deleteProcess(processId);
        } else {
            logger.warn("Not found process [{}]", processId);
        }


        logger.debug("Finish garbage collector for process [{}]", processId);
    }
}