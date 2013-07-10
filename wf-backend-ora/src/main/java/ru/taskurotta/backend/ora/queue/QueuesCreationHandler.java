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
    private final ConcurrentHashMap<String, String> queueNames = new ConcurrentHashMap<String, String>();

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
            String query = "SELECT 1 FROM " + queueName;
            ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return true;
        } catch (SQLException ex) {
            return false;
        } finally {
            closeResources(ps, connection);
        }
    }


    public void registerAndCreateQueue(String actorId) {
        registerAndCreateQueue(actorId, null);
    }


    public void registerAndCreateQueue(String actorId, String queueTableName) {
        synchronized (queueNames) {
            if (!"default".equalsIgnoreCase(queueTableName) && !"default".equalsIgnoreCase(actorId)) {
                if (!queueNames.containsKey(actorId)) {
                    log.info("Creating queue for target [{}]", actorId);
                    long queueId = registerQueue(actorId, queueTableName);
                    if (queueTableName == null) {
                        queueTableName = TABLE_PREFIX + queueId;
                    }
                    queueNames.put(actorId, queueTableName);
                    log.warn("Queues count [{}] ", queueNames.size());
                    if (!queueExists(queueTableName)) {
                        createQueue(queueTableName);
                    }
                }
            }
        }
    }

    public void createQueue(String queueTableName) {
        log.debug("!!!!! Creating queue = " + queueTableName);
        Connection connection = null;
        Statement statement = null;

        if (!"default".equalsIgnoreCase(queueTableName)) {
            try {
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);
                String createQuery = "CREATE TABLE :queue_name \n" +
                        "   (\n" +
                        " TASK_ID VARCHAR(36) NOT NULL ENABLE, \n" +
                        " PROCESS_ID VARCHAR(36) NOT NULL ENABLE, \n" +
                        " STATUS_ID NUMBER NOT NULL ENABLE, \n" +
                        " TASK_LIST VARCHAR(54) NOT NULL ENABLE, \n" +
                        " DATE_START NUMBER, \n" +
                        " INSERT_DATE NUMBER, \n" +
                        " PRIMARY KEY (TASK_ID))";
                String indexQuery = "CREATE INDEX :queue_name_IND ON :queue_name (STATUS_ID, DATE_START)";
                statement = connection.createStatement();
                statement.addBatch(createQuery.replace(":queue_name", queueTableName));
                statement.addBatch(indexQuery.replace(":queue_name", queueTableName));
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
    }

    public Map<String, String> getQueueNames() {
        PreparedStatement ps = null;
        Connection connection = null;
        try {
            Map<String, String> result = new HashMap<String, String>();
            connection = dataSource.getConnection();
            String query = "SELECT * FROM QB$QUEUE_NAMES";
            ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String queueTableName = (rs.getString("queue_table_name") == null) ? TABLE_PREFIX + rs.getLong("queue_id") : rs.getString("queue_table_name");
                result.put(rs.getString("queue_name"), queueTableName);
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
        return registerQueue(queueName, null);
    }


    public long registerQueue(String queueName, String queueTableName) {
        Connection connection = null;
        CallableStatement ps = null;
        try {
            long result;
            connection = dataSource.getConnection();
            String query = "begin\n " +
                    "INSERT INTO QB$QUEUE_NAMES (QUEUE_ID, QUEUE_NAME,QUEUE_TABLE_NAME) VALUES (QB$SEQUENCE.nextval,?, ?) RETURNING QUEUE_ID INTO ?;" +
                    "END;";
            ps = connection.prepareCall(query);
            ps.setString(1, queueName);
            ps.setString(2, queueTableName);
            ps.registerOutParameter(3, Types.BIGINT);
            ps.execute();
            result = ps.getLong(3);
            return result;
        } catch (SQLException ex) {
            log.error("Database error", ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
    }


    public String getTableName(String queueName) {
        return queueNames.get(queueName);
    }
}
