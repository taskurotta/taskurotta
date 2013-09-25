package ru.taskurotta.schedule.storage.ora;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.BackendCriticalException;
import ru.taskurotta.schedule.JobStore;
import ru.taskurotta.schedule.JobVO;
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
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 24.09.13 15:35
 */
public class OraJobStore implements JobStore {

    private static final Logger logger = LoggerFactory.getLogger(OraJobStore.class);

    private DataSource dataSource;
    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<>(TaskContainer.class);


    @Override
    public long addJob(JobVO task) {
        try (Connection connection = dataSource.getConnection();
        CallableStatement cs = connection.prepareCall("BEGIN INSERT INTO TSK_SCHEDULED (NAME, CRON, STATUS, JSON, CREATED, ALLOW_DUPLICATES) VALUES (?, ?, ?, ?, ?, ?) RETURNING ID INTO ?; END;");
        ) {
            cs.setString(1, task.getName());
            cs.setString(2, task.getCron());
            cs.setInt(3, task.getStatus());
            cs.setString(4, (String) taskSerializer.serialize(task.getTask()));
            cs.setTimestamp(5, new Timestamp(new Date().getTime()));
            cs.setString(6, task.isAllowDuplicates()? "Y": "N");
            cs.registerOutParameter(7, Types.NUMERIC);
            cs.execute();

            Long resultId = cs.getLong(7);
            return resultId;
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }
    }

    @Override
    public void removeJob(long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM TSK_SCHEDULED WHERE ID = ? ");
        ) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }
    }

    @Override
    public Collection<Long> getJobIds() {
        Collection<Long> result = new ArrayList<>();
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT ID FROM TSK_SCHEDULED");
        ) {
            rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong("ID"));
            }
            return result;
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public JobVO getJob(long id) {
        ResultSet rs = null;
        JobVO result = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM TSK_SCHEDULED WHERE id = ? ");
        ) {
            ps.setLong(1, id);
            rs = ps.executeQuery();
            while (rs.next()) {
                result = new JobVO();
                result.setId(rs.getLong("ID"));
                result.setName(rs.getString("NAME"));
                result.setCron(rs.getString("CRON"));
                result.setStatus(rs.getInt("STATUS"));
                String allowDuplicates = rs.getString("ALLOW_DUPLICATES");
                if (allowDuplicates!=null && allowDuplicates.equalsIgnoreCase("N")) {
                    result.setAllowDuplicates(false);
                } else {
                    result.setAllowDuplicates(true);
                }
                result.setTask(taskSerializer.deserialize(rs.getString("JSON")));
                return result;
            }
            return result;
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
    }

    @Override
    public void updateJobStatus(long id, int status) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE TSK_SCHEDULED SET STATUS = ? WHERE id = ? ");
        ) {
            ps.setInt(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
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
                throw new BackendCriticalException("Error on closing ResultSet", e);
            }
        }
    }

}
