package ru.taskurotta.backend.ora.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.ora.domain.SimpleTask;
import ru.taskurotta.exception.BackendCriticalException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ru.taskurotta.backend.ora.tools.SqlResourceCloser.*;

/**
 * User: greg
 */

public class OraQueueDao {

    private final static Logger log = LoggerFactory.getLogger(OraQueueDao.class);

    private DataSource dataSource;

    public OraQueueDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void deleteTask(UUID taskId, String queueName) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("delete from " + queueName + " where task_id=? ");
            ps.setString(1, taskId.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

    public void enqueueTask(SimpleTask task, String queueName) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("insert into " + queueName + " (task_id, status_id, task_list, date_start) values (?,?,?,?)");
            ps.setString(1, task.getTaskId().toString());
            ps.setInt(2, task.getStatusId());
            ps.setString(3, task.getTaskList());
            ps.setDate(4, new java.sql.Date(task.getDate().getTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

    public void dequeueTask(UUID taskId, String queueName) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("delete from " + queueName + " where task_id = ?");
            ps.setString(1, taskId.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

    public boolean queueExists(String queueName) {
        PreparedStatement ps = null;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            String query = "SELECT COUNT(*) cnt FROM dba_tables where table_name = ?";
            ps = connection.prepareStatement(query);
            ps.setString(1, queueName.toUpperCase());
            ResultSet rs = ps.executeQuery();
            int count = 0;
            if (rs.next()) {
                count = rs.getInt("cnt");
            }
            return count > 0;
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }

    }

    public void createQueue(String queueName) {
        log.warn("!!!!! Creating queue = " + queueName);
        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            String createQuery = "CREATE TABLE :queue_name \n" +
                    "   (\n" +
                    " TASK_ID VARCHAR(36) NOT NULL ENABLE, \n" +
                    " STATUS_ID NUMBER NOT NULL ENABLE, \n" +
                    " TASK_LIST VARCHAR(54) NOT NULL ENABLE, \n" +
                    " DATE_START DATE, \n" +
                    " PRIMARY KEY (TASK_ID))";
            String indexQuery = "CREATE INDEX :queue_name_IND ON :queue_name (STATUS_ID, DATE_START)";
            statement = connection.createStatement();
            statement.addBatch(createQuery.replace(":queue_name", queueName));
            statement.addBatch(indexQuery.replace(":queue_name", queueName));
            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(statement, connection);
        }
    }

    public UUID pollTask(String queueName) {
        Connection connection = null;
        CallableStatement cs = null;
        try {
            connection = dataSource.getConnection();
            String query = "begin\n" +
                    "UPDATE %s\n" +
                    "SET STATUS_ID = 1\n" +
                    "WHERE\n" +
                    "STATUS_ID = 0\n" +
                    "AND DATE_START <= CURRENT_TIMESTAMP\n" +
                    "AND ROWNUM = 1\n" +
                    "RETURNING TASK_ID INTO ?;END;";
            cs = connection.prepareCall(String.format(query, queueName));
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.execute();
            return (cs.getString(1) != null) ? UUID.fromString(cs.getString(1)) : null;
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(cs, connection);
        }
    }

    public Map<String, Long> getQueueNames() {
        PreparedStatement ps = null;
        Connection connection = null;
        try {
            Map<String, Long> result = new HashMap<String, Long>();
            connection = dataSource.getConnection();
            String query = "SELECT * FROM QB$QUEUE_NAMES";
            ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("queue_name"), rs.getLong("queue_id"));
            }
            return result;
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

    public long registerQueue(String queueName) {
        Connection connection = null;
        CallableStatement ps = null;
        try {
            long result;
            connection = dataSource.getConnection();
            String query = "begin\n INSERT INTO QB$QUEUE_NAMES (QUEUE_ID, QUEUE_NAME) VALUES (QB$SEQUENCE.nextval,?) RETURNING QUEUE_ID INTO ?;END;";
            ps = connection.prepareCall(query);
            ps.setString(1, queueName);
            ps.registerOutParameter(2, Types.BIGINT);
            ps.execute();
            result = ps.getLong(2);
            return result;
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

}

