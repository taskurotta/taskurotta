package ru.taskurotta.backend.ora.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.ora.domain.SimpleTask;
import ru.taskurotta.exception.BackendCriticalException;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.UUID;

import static ru.taskurotta.backend.ora.tools.SqlResourceCloser.closeResources;

/**
 * User: greg
 */

public class OraQueueDao {

    private final static Logger log = LoggerFactory.getLogger(OraQueueDao.class);
    private static final String ORACLE_CONSTRAINT_VIOLATION = "ORA-00001";

    private DataSource dataSource;

    public OraQueueDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public int countTasks(String queueName) {
        Connection connection = null;
        PreparedStatement ps = null;
        int result = 0;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("select count(task_id) cnt from " + queueName);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                result = rs.getInt("cnt");
            }
            return result;
        } catch (SQLException ex) {
            log.error("Count task database error for queue["+queueName+"]", ex);
            throw new BackendCriticalException("Count task database error for queue[\"+queueName+\"]", ex);
        } finally {
            closeResources(ps, connection);
        }
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
            ps = connection.prepareStatement("insert into " + queueName + " (task_id, status_id, task_list, date_start, INSERT_DATE) values (?,?,?,?,?)");
            ps.setString(1, task.getTaskId().toString());
            ps.setInt(2, task.getStatusId());
            ps.setString(3, task.getTaskList());
            Date startTime = new java.sql.Date(task.getDate().getTime());
            ps.setTimestamp(4, new Timestamp(startTime.getTime()));
            ps.setTimestamp(5, new Timestamp(task.getDate().getTime()));
            ps.executeUpdate();

            ps = connection.prepareStatement("UPDATE TASK  SET START_TIME = ? WHERE UUID = ? AND START_TIME IS NULL");
            ps.setTimestamp(1, new Timestamp(startTime.getTime()));
            ps.setString(2, task.getTaskId().toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getMessage().contains(ORACLE_CONSTRAINT_VIOLATION)) {
                log.error(String.format("Constraint violation!!! Task with ID:%s Queue name:%s", task.getTaskId(), queueName));
            } else {
                log.error("Database error", ex);
                throw new BackendCriticalException("Database error", ex);
            }
        } finally {
            closeResources(ps, connection);
        }
    }

    public boolean isQueueExists(String queueName) {
        PreparedStatement ps = null;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            String query = "SELECT 1 FROM " + queueName;
            ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return true;
        } catch (SQLException ex) {
            log.error("Database error", ex);
            return false;
        } finally {
            closeResources(ps, connection);
        }

    }

    public UUID pollTask(String queueName) {
        Connection connection = null;
        CallableStatement cs = null;
        try {
            connection = dataSource.getConnection();
            String query = "begin\n" +
                    "update %qt t\n" +
                    "   set STATUS_ID = 1\n" +
                    " where T.TASK_ID = (select TASK_ID\n" +
                    "                      from (select TT.TASK_ID\n" +
                    "                              from %qt TT\n" +
                    "                             where TT.STATUS_ID = 0\n" +
                    "                               and tt.DATE_START <= current_timestamp\n" +
                    "                               and ROWNUM = 1\n" +
                    "                             order by TT.INSERT_DATE asc))" +
                    "RETURNING TASK_ID INTO ?;END;";
            cs = connection.prepareCall(query.replace("%qt", queueName));
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
}

