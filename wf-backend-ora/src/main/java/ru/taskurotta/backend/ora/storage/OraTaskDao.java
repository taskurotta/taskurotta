package ru.taskurotta.backend.ora.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.serialization.JsonSerializer;
import ru.taskurotta.exception.BackendCriticalException;

/**
 * User: moroz
 * Date: 10.04.13
 */
public class OraTaskDao implements TaskDao {

    private final static Logger log = LoggerFactory.getLogger(OraTaskDao.class);

    private DataSource dataSource;

    private ObjectMapper mapper = new ObjectMapper();

    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<TaskContainer>(TaskContainer.class);
    private JsonSerializer<DecisionContainer> decisionSerializer = new JsonSerializer<DecisionContainer>(DecisionContainer.class);


    public OraTaskDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Override
    public void addDecision(DecisionContainer taskDecision) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("begin add_decision(?,?,?,?,?); end;")
        ) {
            String str = (String) decisionSerializer.serialize(taskDecision);
            ps.setString(1, taskDecision.getTaskId().toString());
            ps.setString(2, taskDecision.getProcessId().toString());
            ps.setString(3, str);
            ps.setInt(4, (taskDecision.containsError()) ? 1 : 0);
            ps.setLong(5, (new Date()).getTime());

            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        }
    }

    @Override
    public DecisionContainer getDecision(UUID taskId) {
        DecisionContainer result = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT DECISION_JSON FROM DECISION WHERE TASK_ID = ?")
        ) {
            ps.setString(1, taskId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String json = rs.getString(1);
                result = decisionSerializer.deserialize(json);
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }
        return result;
    }

    @Override
    public TaskContainer getTask(UUID taskId) {
        TaskContainer result = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT JSON_VALUE FROM TASK WHERE UUID = ?")
        ) {
            ps.setString(1, taskId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String json = rs.getString(1);
                result = taskSerializer.deserialize(json);
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }

        return result;
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO TASK (UUID,JSON_VALUE,NUMBER_OF_ATTEMPTS, ACTOR_ID) VALUES (?,?,?,?)")
        ) {
            ps.setString(1, taskContainer.getTaskId().toString());
            //String str = mapper.writeValueAsString(taskContainer);
            ps.setString(2, (String) taskSerializer.serialize(taskContainer));
            ps.setInt(3, taskContainer.getNumberOfAttempts());
            ps.setString(4, taskContainer.getActorId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }
    }


    @Override
    public boolean isTaskReleased(UUID taskId) {
        boolean result = false;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM DECISION WHERE TASK_ID = ?")
        ) {
            ps.setString(1, taskId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int count = rs.getInt(1);
                result = count > 0;
            }
            return result;
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }

    }

    @Override
    public void updateTask(TaskContainer taskContainer) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE TASK SET JSON_VALUE = ?, NUMBER_OF_ATTEMPTS = ? WHERE UUID = ?")
        ) {
            String str = mapper.writeValueAsString(taskContainer);
            ps.setString(1, str);
            ps.setInt(2, taskContainer.getNumberOfAttempts());
            ps.setString(3, taskContainer.getTaskId().toString());
            ps.executeUpdate();
        } catch (JsonProcessingException ex) {
            log.error("Serialization exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Serialization exception", ex);
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        }
    }
}
