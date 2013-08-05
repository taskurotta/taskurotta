package ru.taskurotta.restarter.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.transport.model.TaskContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 16:51
 */
public class AnalyzerImpl implements Analyzer {

    private static Logger logger = LoggerFactory.getLogger(AnalyzerImpl.class);

    private DataSource dataSource;
    private int processBatchSize;

    private DependencyBackend dependencyBackend;
    private TaskDao taskDao;

    @Override
    public List<TaskContainer> findNotFinishedProcesses(long fromTime) {
        List<TaskContainer> taskContainers = new ArrayList<>();

        String query = "SELECT * FROM (SELECT process_id, start_time, start_task_id FROM process p WHERE state = ? AND start_time >= ? ORDER BY start_time) WHERE ROWNUM <= ?";

        try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, 0);
            preparedStatement.setLong(2, fromTime);
            preparedStatement.setInt(3, processBatchSize);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String processId = resultSet.getString("process_id");
                Date startTime = new Date(resultSet.getLong("start_time"));
                String startTaskId = resultSet.getString("start_task_id");

                logger.info("Found incomplete process [{}] started at [{}]", processId, startTime);

                List<TaskContainer> list = findProcessIncompleteTasks(UUID.fromString(processId), UUID.fromString(startTaskId));

                if (list != null && !list.isEmpty()) {
                    taskContainers.addAll(list);
                }
            }

        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        }

        return taskContainers;
    }

    private List<TaskContainer> findProcessIncompleteTasks(UUID processId, UUID startTaskId) {

        Graph graph = dependencyBackend.getGraph(processId);
        if (graph == null) {
            logger.warn("For processId [{}] not found graph", processId);

            List<TaskContainer> list = Arrays.asList(taskDao.getTask(startTaskId, processId));
            logger.info("For processId [{}] get start task [{}]", processId, list);

            return list;
        }

        Set<UUID> notFinishedTaskIds = graph.getNotFinishedItems();
        logger.debug("For processId [{}] found [{}] not finished taskIds", processId, notFinishedTaskIds);

        List<TaskContainer> taskContainers = new ArrayList<>(notFinishedTaskIds.size());
        for (UUID taskId : notFinishedTaskIds) {

            TaskContainer taskContainer = taskDao.getTask(taskId, processId);
            logger.debug("Found not finished task container [{}]", processId, taskContainer);
            if (taskContainer != null) {
                taskContainers.add(taskContainer);
            }
        }

        logger.info("For processId [{}] found [{}] not finished task containers", processId, taskContainers);

        return taskContainers;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setProcessBatchSize(int processBatchSize) {
        this.processBatchSize = processBatchSize;
    }

    public void setDependencyBackend(DependencyBackend dependencyBackend) {
        this.dependencyBackend = dependencyBackend;
    }

    public void setTaskDao(TaskDao taskDao) {
        this.taskDao = taskDao;
    }
}
