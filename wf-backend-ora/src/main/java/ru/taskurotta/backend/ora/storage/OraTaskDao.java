package ru.taskurotta.backend.ora.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskTargetImpl;

/**
 * User: moroz
 * Date: 10.04.13
 */
public class OraTaskDao implements TaskDao {

    private final static Logger log = LoggerFactory.getLogger(OraTaskDao.class);

    private DataSource dataSource;

    private ObjectMapper mapper = new ObjectMapper();

    public OraTaskDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void addDecision(DecisionContainer taskDecision) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TaskContainer getTask(UUID taskId) {
        TaskContainer result = null;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement ps = connection.prepareStatement("SELECT JSON_VALUE FROM TASK WHERE UUID = ?");
            ps.setString(1, taskId.toString());
            ResultSet rs = ps.executeQuery();
            SimpleModule module = new SimpleModule("test", Version.unknownVersion());
            module.addAbstractTypeMapping(TaskTarget.class, TaskTargetImpl.class);
            mapper.registerModule(module);
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
            final PreparedStatement ps = connection.prepareStatement("INSERT INTO TASK (UUID,IN_PROCESSING,JSON_VALUE) VALUES (?,?,?)");
            ps.setString(1, taskContainer.getTaskId().toString());
            ps.setInt(2, 0);
            String str = mapper.writeValueAsString(taskContainer);
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

//    @Override
//    public void markTaskProcessing(UUID taskId, boolean inProcess) {
//        try {
//            final Connection connection = dataSource.getConnection();
//            final PreparedStatement ps = connection.prepareStatement("UPDATE TASK SET IN_PROCESSING = ? WHERE UUID = ?");
//            ps.setString(2, taskId.toString());
//            ps.setInt(1, inProcess ? 1 : 0);
//            ps.executeUpdate();
//            ps.close();
//            connection.close();
//        } catch (SQLException ex) {
//            log.error("DataBase exception: " + ex.getMessage(), ex);
//        }
//    }
//
//    @Override
//    public boolean isTaskInProgress(UUID taskId) {
//        boolean result = false;
//        try {
//            final Connection connection = dataSource.getConnection();
//            final PreparedStatement ps = connection.prepareStatement("SELECT IN_PROCESSING FROM TASK WHERE UUID = ?");
//            ps.setString(1, taskId.toString());
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                result = (rs.getInt(1) == 1) ? true : false;
//            }
//            ps.close();
//            connection.close();
//
//        } catch (SQLException ex) {
//            log.error("DataBase exception: " + ex.getMessage(), ex);
//        }
//        return result;
//    }

    @Override
    public boolean isTaskReleased(UUID taskId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateTask(TaskContainer taskContainer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
