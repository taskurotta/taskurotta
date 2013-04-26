package ru.taskurotta.backend.ora.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.ora.domain.SimpleTask;
import ru.taskurotta.backend.queue.QueueBackend;

import javax.sql.DataSource;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: moroz
 * Date: 04.04.13
 */
public class OraQueueBackend implements QueueBackend {

    private final static Logger log = LoggerFactory.getLogger(OraQueueBackend.class);

    private static final String TABLE_PREFIX = "qb$";

    private final OraQueueDao dbDAO;

    private final ConcurrentHashMap<String, Long> queueNames = new ConcurrentHashMap<String, Long>();

    private CheckpointService checkpointService = new MemoryCheckpointService();//memory as default, can be overriden with setter

    public OraQueueBackend(DataSource dataSource) {
        dbDAO = new OraQueueDao(dataSource);
        queueNames.putAll(dbDAO.getQueueNames());
        log.warn("Initial queues count [{}] ", queueNames.size());

    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, long startTime, String taskList) {

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

    }

    @Override
    public UUID poll(String actorId, String taskList) {
        String queueName = getTableName(actorId);
        if (queueName != null) {
            final UUID taskId = dbDAO.pollTask(queueName);
            checkpointService.addCheckpoint(new Checkpoint(TimeoutType.TASK_POLL_TO_COMMIT, taskId, actorId, new Date().getTime()));
            dbDAO.deleteTask(taskId, queueName);
            return taskId;
        }
        return null;
    }

    @Override
    public void pollCommit(String actorId, UUID taskId) {
        checkpointService.removeEntityCheckpoints(taskId, TimeoutType.TASK_POLL_TO_COMMIT);
    }


    private String getTableName(String queueName) {
        Long id = queueNames.get(queueName);
        if (id != null) {
            return TABLE_PREFIX + id;
        }
        return null;
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }
}
