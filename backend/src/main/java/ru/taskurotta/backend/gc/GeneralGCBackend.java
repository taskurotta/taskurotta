package ru.taskurotta.backend.gc;

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

    private ProcessBackend processBackend;
    private GraphDao graphDao;
    private TaskDao taskDao;

    public GeneralGCBackend(ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao) {
        this.processBackend = processBackend;
        this.graphDao = graphDao;
        this.taskDao = taskDao;
    }

    public void delete(UUID processId) {

        Graph graph = graphDao.getGraph(processId);
        if (graph != null) {

            Set<UUID> finishedTaskIds = graph.getFinishedItems();

            if (!graph.isFinished()) {
                Set<UUID> notFinishedTaskIds = graph.getNotFinishedItems().keySet();
                deleteTasks(notFinishedTaskIds, processId);
            }

            deleteDecisions(finishedTaskIds, processId);
            deleteTasks(finishedTaskIds, processId);

            graphDao.deleteGraph(processId);
        }

        processBackend.deleteProcess(processId);
    }

    private void deleteDecisions(Set<UUID> taskIds, UUID processId) {
        for (UUID taskId : taskIds) {
            taskDao.deleteDecision(taskId, processId);
        }
    }

    private void deleteTasks(Set<UUID> taskIds, UUID processId) {
        for (UUID taskId : taskIds) {
            taskDao.deleteTask(taskId, processId);
        }
    }
}
