package ru.taskurotta.restartprocesses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.serialization.JsonSerializer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 25.07.13
 * Time: 18:09
 */
public class Restarter {

    private static final Logger logger = LoggerFactory.getLogger(Restarter.class);

    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<>(TaskContainer.class);

    private int period = 1800; // 30 minutes
    private long taskServerStartTime = System.currentTimeMillis();
    private DataSource dataSource;
    private ProcessBackend processBackend;

    public void init() {
        String query = "SELECT " +
                            "p.process_id, " +
                            "p.start_time, " +
                            "t.uuid, " +
                            "t.json_value " +
                        "FROM " +
                            "process p, task t " +
                        "WHERE " +
                            "p.state = ? AND p.start_time >= ? AND p.process_id = t.process_id";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setInt(1, 0);
            preparedStatement.setLong(2, taskServerStartTime / 1000 - period);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String processId = resultSet.getString("process_id");
                Date startTime = new Date(resultSet.getLong("start_time"));
                String taskUUID = resultSet.getString("uuid");
                String json = resultSet.getString("json_value");

                logger.info("Found incomplete process [{}] started at [{}] with start task [{}] = [{}]", processId, startTime, taskUUID, json);

                processBackend.finishProcess(UUID.fromString(processId), null);
                logger.info("Finish process [{}] before restarting", processId);

                TaskContainer taskContainer = taskSerializer.deserialize(json);
                TaskContainer startProcessTaskContainer = new TaskContainer(
                        UUID.randomUUID(), UUID.randomUUID(),
                        taskContainer.getMethod(), taskContainer.getActorId(),
                        taskContainer.getType(), taskContainer.getStartTime(),
                        0, taskContainer.getArgs(), taskContainer.getOptions());
                processBackend.startProcess(startProcessTaskContainer);

                logger.info("Start process from task container [{}]", startProcessTaskContainer);
            }

        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        }
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setProcessBackend(ProcessBackend processBackend) {
        this.processBackend = processBackend;
    }

    public void setTaskServerStartTime(long taskServerStartTime) {
        this.taskServerStartTime = taskServerStartTime;
    }
}
