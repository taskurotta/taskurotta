package ru.taskurotta.backend.gc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;

import java.util.Set;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 05.12.13
 * Time: 12:41
 */
public abstract class AbstractGCTask implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractGCTask.class);

    protected ProcessBackend processBackend;
    protected GraphDao graphDao;
    protected TaskDao taskDao;

    protected AbstractGCTask(ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao) {
        this.processBackend = processBackend;
        this.graphDao = graphDao;
        this.taskDao = taskDao;
    }

    protected void gc(UUID processId) {

        if (processId == null) {
            logger.warn("ProcessId for garbage collector is null");
            return;
        }

        logger.trace("Start garbage collector for process [{}]", processId);

        Graph graph = graphDao.getGraph(processId);

        if (graph == null) {
            logger.error("Not found graph for process [{}], stop garbage collector for this process", processId);
            return;
        }

        if (!graph.isFinished()) {
            logger.error("Graph for process [{}] isn't finished, stop garbage collector for this process", processId);
            return;
        }

        Set<UUID> finishedItems = graph.getFinishedItems();
        taskDao.deleteDecisions(finishedItems, processId);
        taskDao.deleteTasks(finishedItems, processId);

        graphDao.deleteGraph(processId);

        processBackend.deleteProcess(processId);

        logger.debug("Finish garbage collector for process [{}]", processId);
    }
}