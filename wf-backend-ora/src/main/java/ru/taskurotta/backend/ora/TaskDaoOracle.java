package ru.taskurotta.backend.ora;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.ora.dao.DbConnect;
import ru.taskurotta.backend.ora.domain.SimpleTask;
import ru.taskurotta.backend.ora.queue.OraQueueDao;
import ru.taskurotta.server.memory.TaskDaoMemory;
import ru.taskurotta.server.model.TaskObject;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: greg
 */
public class TaskDaoOracle extends TaskDaoMemory {

    private final static Logger log = LoggerFactory.getLogger(TaskDaoOracle.class);

    private final OraQueueDao dbDAO;
    private final ConcurrentHashMap<String, Long> queueNames = new ConcurrentHashMap<String, Long>();

    public TaskDaoOracle(DbConnect dbConnect) {
        dbDAO = new OraQueueDao(dbConnect.getDataSource());
        try {
            queueNames.putAll(dbDAO.getQueueNames());
            log.warn("!!!!! Initial queue size = " + queueNames.size());
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void init() {

    }

    @Override
    protected void addTaskToQueue(TaskObject taskObj) {
        //super.add(taskObj);
        try {
            String queueName = getQueueName(taskObj.getTarget().getName(), taskObj.getTarget().getVersion());
            log.debug("addTaskToQueue taskId = [{}]", taskObj.getTaskId());

            synchronized (queueNames) {
                if (!queueNames.containsKey(queueName)) {
                    log.warn("Create queue for target [{}]", taskObj.getTarget().getName());
                    long queueId = dbDAO.registerQueue(queueName);
                    queueNames.put(queueName, queueId);
                    dbDAO.createQueue("qb$" + queueId);
                }
            }

            dbDAO.enqueueTask(new SimpleTask(taskObj.getTaskId(), new Date(taskObj.getStartTime()), 0, null), getTableName(queueName));
        } catch (SQLException ex) {
            log.error("Database error", ex);
        }
    }

    @Override
    public TaskObject pull(ActorDefinition actorDefinition) {
        String queueName = getTableName(getQueueName(actorDefinition.getName(), actorDefinition.getVersion()));
        if (queueName != null) {
            try {
                final UUID taskId = dbDAO.pollTask(queueName);
                return taskMap.get(taskId);
            } catch (SQLException ex) {
                log.error("Database error", ex);
            }
        }
        return null;

    }

    private String getTableName(String queueName) {
        Long id = queueNames.get(queueName);
        if (id != null) {
            return "qb$" + id;
        }
        return null;
    }


}
