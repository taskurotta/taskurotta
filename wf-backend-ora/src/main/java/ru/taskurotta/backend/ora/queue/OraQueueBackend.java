package ru.taskurotta.backend.ora.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Profiled;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.QueuedTaskVO;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.ora.domain.SimpleTask;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.exception.BackendCriticalException;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * User: moroz, dudin
 * Date: 04.04.13
 */
public class OraQueueBackend implements QueueBackend, QueueInfoRetriever {

    private final static Logger log = LoggerFactory.getLogger(OraQueueBackend.class);

    private final OraQueueDao dbDAO;

    private final ConfigBackend configBackend;

    private boolean validating = false;
    private boolean autocreating = true;
    private QueuesCreationHandler queuesCreationHandler;

    private CheckpointService checkpointService = new MemoryCheckpointService();//memory as default, can be overriden with setter

    public OraQueueBackend(DataSource dataSource, ConfigBackend configBackend, boolean validating, boolean autocreating) {
        this.validating = validating;
        this.autocreating = autocreating;
        this.configBackend = configBackend;
        dbDAO = new OraQueueDao(dataSource);

        //Load all queue table names
        queuesCreationHandler = new QueuesCreationHandler(dataSource);
        queuesCreationHandler.initQueueNames();

        createQueueTablesw();

        if (validating) {//Validate configured queues existence at oracle
            validateQueues();
        }

        log.info("OraQueueBackend initialized with [{}] actor configs", configBackend.getAllActorPreferences() != null ? configBackend.getAllActorPreferences().length : 0);

    }

    public OraQueueBackend(DataSource dataSource, ConfigBackend configBackend) {
        this(dataSource, configBackend, false, true);
    }

    private void createQueueTablesw() {
        if (configBackend.getAllActorPreferences() == null || configBackend.getAllActorPreferences().length == 0) {
            throw new BackendCriticalException("There are no actor preferences configured for this TaskServer!");
        }

        for (ActorPreferences actorPref : configBackend.getAllActorPreferences()) {
            queuesCreationHandler.registerAndCreateQueue(actorPref.getId(), actorPref.getQueueName());
        }
    }

    private void validateQueues() throws BackendCriticalException {

        if (configBackend.getAllActorPreferences() == null || configBackend.getAllActorPreferences().length == 0) {
            throw new BackendCriticalException("There are no actor preferences configured for this TaskServer!");
        }

        Set<String> errorQueues = new HashSet<String>();
        for (ActorPreferences actorPref : configBackend.getAllActorPreferences()) {
            if (actorPref.getId().equalsIgnoreCase("default")) {//Reserved actor config -> just skip it
                continue;
            }

            if (actorPref.getQueueName() == null) {
                throw new BackendCriticalException("There are no queue configured for actor[" + actorPref.getId() + "]");
            }

            if (!dbDAO.isQueueExists(actorPref.getQueueName())) {
                errorQueues.add(actorPref.getQueueName());
            }
        }
        if (errorQueues.size() > 0) {
            throw new BackendCriticalException("Cannot validate queues (require manual creation?)[" + errorQueues + "]");
        }

    }


    @Override
    @Profiled
    public void enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {

        log.debug("addTaskToQueue taskId = [{}]", taskId);

        String tableName = getTableName(actorId, taskList);

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }
        dbDAO.enqueueTask(new SimpleTask(taskId, new Date(startTime), 0, null), tableName);

    }

    @Override
    @Profiled
    public UUID poll(String actorId, String taskList) {
        String queueName = getTableName(actorId, taskList);

        final UUID taskId = dbDAO.pollTask(queueName);
        if (taskId != null) {//there can be no tasks in the queue
            Checkpoint pollCheckpoint = new Checkpoint(TimeoutType.TASK_POLL_TO_COMMIT, taskId, actorId, System.currentTimeMillis());
            checkpointService.addCheckpoint(pollCheckpoint);
            dbDAO.deleteTask(taskId, queueName);
        }
        return taskId;
    }

    @Override
    @Profiled
    public void pollCommit(String actorId, UUID taskId) {
        checkpointService.removeEntityCheckpoints(taskId, TimeoutType.TASK_POLL_TO_COMMIT);
    }


    private String getTableName(String actorId, String taskList) {
        ActorPreferences actorPreferences = configBackend.getActorPreferences(actorId);
        String queueName = actorPreferences != null ? actorPreferences.getQueueName() : null;
        log.debug("actorPreferences getted for actor[{}] is [{}], queueName getted is[{}]", actorId, actorPreferences, queueName);

        //Create queue if need
        if (queueName == null && autocreating) {
            queuesCreationHandler.registerAndCreateQueue(actorId);
            queueName = queuesCreationHandler.getTableName(actorId);
        }

        if (queueName == null) {
            throw new BackendCriticalException("Cannot determine queue for: actorId[" + actorId + "], taskList[" + taskList + "]!");
        }

        return queueName;
    }


    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        return dbDAO.getQueueList(pageNum, pageSize, true);
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return dbDAO.countTasks(queueName);
    }

    @Override
    @Profiled
    public GenericPage<QueuedTaskVO> getQueueContent(String queueName, int pageNum, int pageSize) {
        List<QueuedTaskVO> result = null;
        GenericPage<QueueItem> tmpPage = dbDAO.getQueueContent(queueName, pageNum, pageSize);

        if (tmpPage != null && !tmpPage.getItems().isEmpty()) {
            result = new ArrayList<QueuedTaskVO>();
            for (QueueItem qi : tmpPage.getItems()) {
                QueuedTaskVO qt = new QueuedTaskVO();
                qt.setId(qi.getId());
                qt.setTaskList(qi.getTaskList());
                qt.setInsertTime(qi.getInsertDate() != null ? qi.getInsertDate().getTime() : 0);
                qt.setStartTime(qi.getStartDate() != null ? qi.getStartDate().getTime() : 0);
                result.add(qt);
            }
        }

        return new GenericPage<QueuedTaskVO>(result, tmpPage.getPageNumber(), tmpPage.getPageSize(), tmpPage.getTotalCount());
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        Map<String, Integer> result = new HashMap<>();
        GenericPage<String> queues = dbDAO.getQueueList(1, 0, false);
        for (String queue : queues.getItems()) {
            result.put(queue, dbDAO.getHoveringCount(queue, periodSize));
        }
        return result;
    }
}
