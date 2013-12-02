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
 * Date: 29.11.13
 * Time: 12:22
 */
public class GeneralGCBackend {

    private static final Logger logger = LoggerFactory.getLogger(GeneralGCBackend.class);

    private ProcessBackend processBackend;
    private GraphDao graphDao;
    private TaskDao taskDao;

    public GeneralGCBackend(ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao) {
        this.processBackend = processBackend;
        this.graphDao = graphDao;
        this.taskDao = taskDao;
    }

    public void delete(UUID processId) {

        logger.trace("Try to garbage collector for process [{}]", processId);

        Graph graph = graphDao.getGraph(processId);
        if (graph != null) {

            Set<UUID> finishedTaskIds = graph.getFinishedItems();

            if (!graph.isFinished()) {
                Set<UUID> notFinishedTaskIds = graph.getNotFinishedItems().keySet();
                taskDao.deleteTasks(notFinishedTaskIds, processId);
            }

            taskDao.deleteDecisions(finishedTaskIds, processId);
            taskDao.deleteTasks(finishedTaskIds, processId);

            graphDao.deleteGraph(processId);
        }

        processBackend.deleteProcess(processId);

        logger.debug("Done garbage collector for [{}]", processId);
    }
}
