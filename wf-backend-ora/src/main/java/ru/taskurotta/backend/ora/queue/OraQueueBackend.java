package ru.taskurotta.backend.ora.queue;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.ora.dao.DbConnect;
import ru.taskurotta.backend.ora.domain.SimpleTask;
import ru.taskurotta.backend.queue.QueueBackend;

/**
 * User: moroz
 * Date: 04.04.13
 */
public class OraQueueBackend implements QueueBackend {

    private final static Logger log = LoggerFactory.getLogger(OraQueueBackend.class);

    private static final String TABLE_PREFIX = "qb$";

    private final OraQueueDao dbDAO;

    private final ConcurrentHashMap<String, Long> queueNames = new ConcurrentHashMap<String, Long>();

    public OraQueueBackend(DbConnect dbConnect) {
        dbDAO = new OraQueueDao(dbConnect.getDataSource());
        try {
            queueNames.putAll(dbDAO.getQueueNames());
            log.warn("Initial queues count [{}] ", queueNames.size());
        } catch (SQLException e) {
            log.error("Error creating db connection", e);
        }
    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, long startTime, String taskList) {
        try {
            //String queueName = getQueueName(actorDefinition.getName(), actorDefinition.getVersion());
            log.debug("addTaskToQueue taskId = [{}]", taskId);

            synchronized (queueNames) {
                if (!queueNames.containsKey(actorId)) {
                    log.warn("Create queue for target [{}]", actorId);
                    long queueId = dbDAO.registerQueue(actorId);
                    queueNames.put(actorId, queueId);
                    log.warn("Queues count [{}] ", queueNames.size());
                    dbDAO.createQueue(TABLE_PREFIX + queueId);
                }
            }

            dbDAO.enqueueTask(new SimpleTask(taskId, new Date(startTime), 0, null), getTableName(actorId));
        } catch (SQLException ex) {
            log.error("Database error", ex);
        }
    }

    @Override
    public UUID poll(String actorId, String taskList) {
        String queueName = getTableName(actorId);
        if (queueName != null) {
            try {
                final UUID taskId = dbDAO.pollTask(queueName);
                return taskId;
            } catch (SQLException ex) {
                log.error("Database error", ex);
            }
        }
        return null;
    }

    @Override
    public void pollCommit(String actorId, UUID taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    private String getTableName(String queueName) {
        Long id = queueNames.get(queueName);
        if (id != null) {
            return TABLE_PREFIX + id;
        }
        return null;
    }
}
