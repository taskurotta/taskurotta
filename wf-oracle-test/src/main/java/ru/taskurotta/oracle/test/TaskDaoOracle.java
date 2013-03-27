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
            final String taskName = getQueueName(taskObj.getTarget().getName(), taskObj.getTarget().getVersion());
            final String queueName = getMD5(taskName);
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

    private String getMD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    @Override
    protected String getQueueName(String actorDefinitionName, String actorDefinitionVersion) {
        return super.getQueueName(actorDefinitionName, actorDefinitionVersion).replace("#", "_");
    }
}
