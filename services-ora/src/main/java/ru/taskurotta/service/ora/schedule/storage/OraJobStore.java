package ru.taskurotta.service.ora.schedule.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.schedule.JobConstants;
import ru.taskurotta.service.schedule.storage.JobStore;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.serialization.JsonSerializer;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Oracle implementation of Scheduled tasks storage
 * User: dimadin
 * Date: 24.09.13 15:35
 */
public class OraJobStore implements JobStore {

    private static final Logger logger = LoggerFactory.getLogger(OraJobStore.class);

    private DataSource dataSource;
    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<>(TaskContainer.class);

    @Override
    public long add(JobVO task) {
        try (Connection connection = dataSource.getConnection();
            CallableStatement cs = connection.prepareCall("BEGIN INSERT INTO TSK_SCHEDULED (NAME, CRON, STATUS, JSON, CREATED, QUEUE_LIMIT, MAX_ERRORS, ERR_COUNT) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING ID INTO ?; END;")
        ) {
            cs.setString(1, task.getName());
            cs.setString(2, task.getCron());
            cs.setInt(3, task.getStatus());
            cs.setString(4, (String) taskSerializer.serialize(task.getTask()));
            cs.setTimestamp(5, new Timestamp(new Date().getTime()));
            cs.setInt(6, task.getQueueLimit());
            cs.setInt(7, task.getMaxErrors());
            cs.setInt(8, task.getErrorCount());

            cs.registerOutParameter(9, Types.NUMERIC);
            cs.execute();

            return cs.getLong(9);
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public void remove(long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM TSK_SCHEDULED WHERE ID = ? ")
        ) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public Collection<Long> getKeys() {
        Collection<Long> result = new ArrayList<>();
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT ID FROM TSK_SCHEDULED")
        ) {
            rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong("ID"));
            }
            return result;
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public JobVO get(long id) {
        ResultSet rs = null;
        JobVO result = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM TSK_SCHEDULED WHERE id = ? ")
        ) {
            ps.setLong(1, id);
            rs = ps.executeQuery();
            while (rs.next()) {
                result = new JobVO();
                result.setId(rs.getLong("ID"));
                result.setName(rs.getString("NAME"));
                result.setCron(rs.getString("CRON"));
                result.setStatus(rs.getInt("STATUS"));
                result.setQueueLimit(rs.getInt("QUEUE_LIMIT"));
                result.setMaxErrors(rs.getInt("MAX_ERRORS"));
                result.setErrorCount(rs.getInt("ERR_COUNT"));
                result.setLastError(rs.getString("LAST_ERR_MESSAGE"));
                result.setTask(taskSerializer.deserialize(rs.getString("JSON")));
            }
            return result;
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public void updateJobStatus(long id, int status) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE TSK_SCHEDULED SET STATUS = ? WHERE id = ? ")
        ) {
            ps.setInt(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public void update(JobVO jobVO, long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE TSK_SCHEDULED SET NAME = ?, CRON = ?, STATUS = ?, JSON = ?, CREATED = ?, QUEUE_LIMIT = ?, MAX_ERRORS = ? WHERE id = ? ")
        ) {
            ps.setString(1, jobVO.getName());
            ps.setString(2, jobVO.getCron());
            ps.setInt(3, jobVO.getStatus());
            ps.setString(4, (String) taskSerializer.serialize(jobVO.getTask()));
            ps.setTimestamp(5, new Timestamp(new Date().getTime()));
            ps.setInt(6, jobVO.getQueueLimit());
            ps.setInt(7, jobVO.getMaxErrors());
            ps.setLong(8, id);

            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public int getJobStatus(long jobId) {
        ResultSet rs = null;
        int result = JobConstants.STATUS_UNDEFINED;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT status FROM TSK_SCHEDULED WHERE id = ? ")
        ) {
            ps.setLong(1, jobId);
            rs = ps.executeQuery();
            while (rs.next()) {
                result = rs.getInt("STATUS");
            }
            return result;
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public void updateErrorCount(long jobId, int count, String message) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE TSK_SCHEDULED SET ERR_COUNT = ?, LAST_ERR_MESSAGE = ? WHERE id = ? ")
        ) {
            ps.setInt(1, count);
            ps.setString(2, message);
            ps.setLong(3, jobId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new ServiceCriticalException("Error on closing ResultSet", e);
            }
        }
    }



}
