package ru.taskurotta.backend.ora.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.ora.dao.DbConnect;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;

/**
 * User: moroz
 * Date: 10.04.13
 */
public class OraTaskDao implements TaskDao {

    private final static Logger log = LoggerFactory.getLogger(OraTaskDao.class);

    private DataSource dataSource;

    private ObjectMapper mapper = new ObjectMapper();

    public OraTaskDao(DbConnect dbConnect) {
        this.dataSource = dbConnect.getDataSource();
    }

    @Override
    public void addDecision(DecisionContainer taskDecision) {
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement ps = connection.prepareStatement("INSERT INTO DECISION (TASK_ID,PROCESS_ID,DESICION_JSON) VALUES (?,?,?)");
            ps.setString(1, taskDecision.getTaskId().toString());
            ps.setString(2, taskDecision.getProcessId().toString());
            String str = mapper.writeValueAsString(taskDecision);
            ps.setString(3, str);
            ps.executeUpdate();
            ps.close();
            connection.close();
        } catch (JsonProcessingException ex) {
            log.error("Serialization exception: " + ex.getMessage(), ex);
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
        }
    }

    @Override
    public DecisionContainer getDecision(UUID taskId) {
        DecisionContainer result = null;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement ps = connection.prepareStatement("SELECT  DESICION_JSON FROM DECISION WHERE TASK_ID = ?");
            ps.setString(1, taskId.toString());
            ResultSet rs = ps.executeQuery();
//            SimpleModule module = new SimpleModule("test", Version.unknownVersion());
//            module.addAbstractTypeMapping(TaskTarget.class, TaskTargetImpl.class);
//            mapper.registerModule(module);
            try {
                while (rs.next()) {
                    String json = rs.getString(1);
                    result = mapper.readValue(json, DecisionContainer.class);
                }
            } catch (IOException ex) {
                log.error("Serialization exception: " + ex.getMessage(), ex);
            }
            ps.close();
            connection.close();

        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
        }

        return result;
    }

    @Override
    public TaskContainer getTask(UUID taskId) {
        TaskContainer result = null;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement ps = connection.prepareStatement("SELECT JSON_VALUE FROM TASK WHERE UUID = ?");
            ps.setString(1, taskId.toString());
            ResultSet rs = ps.executeQuery();
//            SimpleModule module = new SimpleModule("test", Version.unknownVersion());
//            module.addAbstractTypeMapping(TaskTarget.class, TaskTargetImpl.class);
//            mapper.registerModule(module);
            try {
                while (rs.next()) {
                    String json = rs.getString(1);
                    result = mapper.readValue(json, TaskContainer.class);
                }
            } catch (IOException ex) {
                log.error("Serialization exception: " + ex.getMessage(), ex);
            }
            ps.close();
            connection.close();

        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
        }

        return result;
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement ps = connection.prepareStatement("INSERT INTO TASK (UUID,JSON_VALUE) VALUES (?,?)");
            ps.setString(1, taskContainer.getTaskId().toString());
            String str = mapper.writeValueAsString(taskContainer);
            ps.setString(2, str);
            ps.executeUpdate();
            ps.close();
            connection.close();
        } catch (JsonProcessingException ex) {
            log.error("Serialization exception: " + ex.getMessage(), ex);
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
        }
    }


    @Override
    public boolean isTaskReleased(UUID taskId) {
        boolean result = false;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM DECISION WHERE TASK_ID = ?");
            ps.setString(1, taskId.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int count = rs.getInt(1);
                result = count > 0;
            }

            ps.close();
            connection.close();

        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
        }

        return result;
    }

    @Override
    public void updateTask(TaskContainer taskContainer) {
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement ps = connection.prepareStatement("UPDATE TASK SET JSON_VALUE = ? WHERE UUID = ?");
            String str = mapper.writeValueAsString(taskContainer);
            ps.setString(1, str);
            ps.setString(2, taskContainer.getTaskId().toString());
            ps.executeUpdate();
            ps.close();
            connection.close();
        } catch (JsonProcessingException ex) {
            log.error("Serialization exception: " + ex.getMessage(), ex);
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
        }
    }
}
