package ru.taskurotta.oracle.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.oracle.test.domain.SimpleTask;
import ru.taskurotta.server.memory.TaskDaoMemory;
import ru.taskurotta.server.model.TaskObject;
import ru.taskurotta.util.ActorDefinition;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User: greg
 */
public class TaskDaoOracle extends TaskDaoMemory {

    private final static Logger log = LoggerFactory.getLogger(TaskDaoOracle.class);

    private final DbDAO dbDAO;
    private final Set<String> queueNames;

    public TaskDaoOracle(DbConnect dbConnect) {
        dbDAO = new DbDAO(dbConnect.getDataSource());
        queueNames = new HashSet<String>();
    }

    @Override
    public void add(TaskObject taskObj) {
        super.add(taskObj);
        try {
            final String queueName = getQueueName(taskObj.getTarget().getName(), taskObj.getTarget().getVersion());
            if (!queueNames.contains(queueName) || !dbDAO.queueExists(queueName)) {
                dbDAO.createQueue(queueName);
                queueNames.add(queueName);
            }
            dbDAO.enqueueTask(SimpleTask.createFromTaskObject(taskObj), queueName);
        } catch (SQLException ex) {
            log.error("Database error", ex);
        }
    }

    @Override
    public TaskObject pull(ActorDefinition actorDefinition) {
        String queueName = getQueueName(actorDefinition.getName(), actorDefinition.getVersion());
        try {
            final UUID taskId = dbDAO.pullTask(queueName);
            return taskMap.get(taskId);
        } catch (SQLException ex) {
            log.error("Database error", ex);
            return null;
        }

    }
}
