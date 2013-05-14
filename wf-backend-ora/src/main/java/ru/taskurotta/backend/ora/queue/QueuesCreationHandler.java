package ru.taskurotta.backend.ora.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.BackendCriticalException;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ru.taskurotta.backend.ora.tools.SqlResourceCloser.closeResources;

/**
 * Handler containing logic for queues auto creation.
 * User: dimadin
 * Date: 14.05.13 12:06
 */
public class QueuesCreationHandler {
    private final static Logger log = LoggerFactory.getLogger(QueuesCreationHandler.class);

    private static final String TABLE_PREFIX = "qb$";
    private final ConcurrentHashMap<String, Long> queueNames = new ConcurrentHashMap<String, Long>();

    private DataSource dataSource;

    public QueuesCreationHandler(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initQueueNames() {
        queueNames.putAll(getQueueNames());
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


    public void registerAndCreateQueue(String actorId) {
        synchronized (queueNames) {
            if (!queueNames.containsKey(actorId)) {
                log.warn("Create queue for target [{}]", actorId);
                long queueId = registerQueue(actorId);
                queueNames.put(actorId, queueId);
                log.warn("Queues count [{}] ", queueNames.size());
                createQueue(TABLE_PREFIX + queueId);
            }
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
                    " DATE_START TIMESTAMP, \n" +
                    " INSERT_DATE TIMESTAMP, \n" +
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

    public String getTableName(String queueName) {
        Long id = queueNames.get(queueName);
        if (id != null) {
            return TABLE_PREFIX + id;
        }
        return null;
    }



}
