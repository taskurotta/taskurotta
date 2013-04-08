package ru.taskurotta.backend.ora.queue;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.ora.dao.DbConnect;
import ru.taskurotta.backend.ora.dao.OraQueueDao;
import ru.taskurotta.backend.ora.domain.SimpleTask;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: moroz
 * Date: 04.04.13
 */
public class OraQueueBackend implements QueueBackend {

    private final static Logger log = LoggerFactory.getLogger(OraQueueBackend.class);

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
    public void enqueueItem(ActorDefinition actorDefinition, UUID taskId, long startTime) {
        try {
            String queueName = getQueueName(actorDefinition.getName(), actorDefinition.getVersion());
            log.debug("addTaskToQueue taskId = [{}]", taskId);

            synchronized (queueNames) {
                if (!queueNames.containsKey(queueName)) {
                    log.warn("Create queue for target [{}]", actorDefinition.getName());
                    long queueId = dbDAO.registerQueue(queueName);
                    queueNames.put(queueName, queueId);
                    log.warn("Queues count [{}] ", queueNames.size());
                    dbDAO.createQueue("qb$" + queueId);
                }
            }

            dbDAO.enqueueTask(new SimpleTask(taskId, new Date(startTime), 0, null), getTableName(queueName));
        } catch (SQLException ex) {
            log.error("Database error", ex);
        }
    }

    @Override
    public UUID poll(ActorDefinition actorDefinition) {
        String queueName = getTableName(getQueueName(actorDefinition.getName(), actorDefinition.getVersion()));
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
    public void pollCommit(UUID taskId) {
        //There is no need to implement this method for Oracle.
    }

    public static String getQueueName(String actorDefinitionName, String actorDefinitionVersion) {
        return actorDefinitionName + '#' + actorDefinitionVersion;
    }

    private String getTableName(String queueName) {
        Long id = queueNames.get(queueName);
        if (id != null) {
            return "qb$" + id;
        }
        return null;
    }
}
