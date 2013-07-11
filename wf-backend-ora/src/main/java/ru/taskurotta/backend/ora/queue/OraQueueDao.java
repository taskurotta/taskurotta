package ru.taskurotta.backend.ora.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.ora.domain.SimpleTask;
import ru.taskurotta.backend.ora.tools.PagedQueryBuilder;
import ru.taskurotta.backend.queue.TaskQueueItem;
import ru.taskurotta.exception.BackendCriticalException;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
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
            if (rs.next()) {
                result = rs.getInt("cnt");
            }
            return result;
        } catch (SQLException ex) {
            log.error("Count task database error for queue[" + queueName + "]", ex);
            throw new BackendCriticalException("Count task database error for queue["+queueName+"]", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

    public void deleteTask(UUID taskId, UUID processId, String queueName) {
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
            ps = connection.prepareStatement("insert into " + queueName + " (task_id, process_id, status_id, task_list, date_start, insert_date) values (?,?,?,?,?,?)");
            ps.setString(1, task.getTaskId().toString());
            ps.setString(2, task.getProcessId().toString());
            ps.setInt(3, task.getStatusId());
            ps.setString(4, task.getTaskList());
            ps.setLong(5, task.getStartTime());
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();


            //TODO: it isnt right to modify task backend table in queue backend class. They should be independent backends by design
            /////////////////////// //TODO: start time should be simple number, not date or timestamp
            ps = connection.prepareStatement("UPDATE TASK  SET START_TIME = ? WHERE UUID = ? AND START_TIME IS NULL");
            ps.setTimestamp(1, new Timestamp(task.getStartTime()));
            ps.setString(2, task.getTaskId().toString());
            ps.executeUpdate();
            ///////////////////////

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

    public TaskQueueItem pollTask(String queueName) {
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
                    "                               and tt.DATE_START <= %ds \n" +
                    "                               and ROWNUM = 1\n" +
                    "                             order by TT.INSERT_DATE asc))" +
                    "RETURNING TASK_ID INTO ?;END;";
            cs = connection.prepareCall(query.replace("%qt", queueName).replace("%ds", String.valueOf(System.currentTimeMillis())));
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.execute();
            UUID taskId =  (cs.getString(1) != null) ? UUID.fromString(cs.getString(1)) : null;
            return taskId!=null? getTaskQueueItem(queueName, taskId): null;
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(cs, connection);
        }
    }

    private TaskQueueItem getTaskQueueItem(String queueName, UUID taskId) {
        TaskQueueItem result = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM "+queueName+" WHERE TASK_ID = ? ")
        ) {
            ps.setString(1, taskId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = new TaskQueueItem();
                result.setTaskId(UUID.fromString(rs.getString("task_id")));
                result.setProcessId(UUID.fromString(rs.getString("process_id")));
                result.setTaskList(rs.getString("task_list"));
                result.setEnqueueTime(rs.getLong("insert_date"));
                result.setStartTime(rs.getLong("date_start"));
            }
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        }
        return result;
    }

    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            List<TaskQueueItem> tmpresult = new ArrayList<TaskQueueItem>();
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(PagedQueryBuilder.createPagesQuery("select * from " + queueName));
            int startIndex = (pageNum - 1) * pageSize + 1;
            int endIndex = startIndex + pageSize - 1;
            ps.setInt(1, endIndex);
            ps.setInt(2, startIndex);
            long totalCount = 0;
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TaskQueueItem qi = new TaskQueueItem();

                String taskIdStr = rs.getString("task_id");
                qi.setTaskId(taskIdStr != null ? UUID.fromString(taskIdStr) : null);
                String processIdStr = rs.getString("process_id");
                qi.setProcessId(processIdStr != null ? UUID.fromString(processIdStr) : null);
                qi.setTaskList(rs.getString("task_list"));

                Timestamp startTime = rs.getTimestamp("date_start");
                qi.setStartTime(startTime != null ? startTime.getTime() : -1);

                Timestamp enqueueTime = rs.getTimestamp("insert_date");
                qi.setEnqueueTime(enqueueTime != null ? enqueueTime.getTime() : -1);
                totalCount = rs.getLong("cnt");
                tmpresult.add(qi);

            }
            return new GenericPage(tmpresult, pageNum, pageSize, totalCount);
        } catch (SQLException ex) {
            log.error("Queue[" + queueName + "] content extraction error!", ex);
            throw new BackendCriticalException("Queue[" + queueName + "] content extraction error!", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

    public GenericPage<String> getQueueList(int pageNum, int pageSize, boolean paging) {
        PreparedStatement ps = null;
        Connection connection = null;

        List<String> tmpResult = new ArrayList<>();
        long totalCount = 0;
        try {
            String query = "SELECT queue_table_name FROM QB$QUEUE_NAMES";
            if (paging) {
                query = PagedQueryBuilder.createPagesQuery("SELECT queue_table_name FROM QB$QUEUE_NAMES");
            }
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(query);
            if (paging) {
                int startIndex = (pageNum - 1) * pageSize + 1;
                int endIndex = startIndex + pageSize - 1;
                ps.setInt(1, endIndex);
                ps.setInt(2, startIndex);
            }
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String queueName = rs.getString("queue_table_name");
                if (!"default".equals(queueName)) {
                    tmpResult.add(queueName);
                }
                if (paging) {
                    totalCount = rs.getLong("cnt");
                }
            }
        } catch (SQLException ex) {
            log.error("Get queue page error", ex);
            throw new BackendCriticalException("Can't  get queue list", ex);
        } finally {
            closeResources(ps, connection);
        }

        return new GenericPage<String>(tmpResult, pageNum, pageSize, (paging) ? totalCount : tmpResult.size());
    }


    public int getHoveringCount(String queueName, float periodSize) {
        int result = 0;
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            String query = "select COUNT(TASK_ID) cnt from " + queueName + "  where date_start < sysdate - ?";
            ps = connection.prepareStatement(query);
            ps.setFloat(1, periodSize);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = rs.getInt("cnt");
            }
        } catch (SQLException ex) {
            log.error("Get queue page error", ex);
            throw new BackendCriticalException("Can't  get queue list", ex);
        } finally {
            closeResources(ps, connection);
        }
        return result;
    }
}

