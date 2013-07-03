package ru.taskurotta.backend.hz.queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.InstanceEvent;
import com.hazelcast.core.InstanceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Profiled;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.QueuedTaskVO;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.hz.Constants;
import ru.taskurotta.backend.queue.QueueBackend;

/**
 * Created by void, dudin 07.06.13 11:00
 */
public class HzQueueBackend implements QueueBackend, QueueInfoRetriever, InstanceListener {

    private final static Logger logger = LoggerFactory.getLogger(HzQueueBackend.class);

    private int pollDelay = 60;
    private TimeUnit pollDelayUnit = TimeUnit.SECONDS;
    private CheckpointService checkpointService = new MemoryCheckpointService();//default, can be overridden with setter

    //Hazelcast specific
    private HazelcastInstance hazelcastInstance;
    private String queueListName = Constants.DEFAULT_QUEUE_LIST_NAME;

    public HzQueueBackend(int pollDelay, TimeUnit pollDelayUnit, HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        this.pollDelay = pollDelay;
        this.pollDelayUnit = pollDelayUnit;

        this.hazelcastInstance.addInstanceListener(this);
        logger.debug("HzQueueBackend created and registered as Hazelcast instance listener");
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        Set<String> queueNamesSet = hazelcastInstance.<String>getSet(queueListName);
        logger.debug("Stored queue names for queue backend are [{}]", new ArrayList(queueNamesSet));
        List<String> result = new ArrayList<>(pageSize);
        String[] queueNames = queueNamesSet.toArray(new String[queueNamesSet.size()]);
        if (queueNames.length > 0) {
            int pageStart = (pageNum - 1) * pageSize;
            int pageEnd = pageSize * pageNum >= queueNames.length ? queueNames.length : pageSize * pageNum;
            result.addAll(Arrays.asList(queueNames).subList(pageStart, pageEnd));
        }
        return new GenericPage<>(result, pageNum, pageSize, queueNames.length);
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return hazelcastInstance.getQueue(queueName).size();
    }

    @Override
    public GenericPage<QueuedTaskVO> getQueueContent(String queueName, int pageNum, int pageSize) {
        List<QueuedTaskVO> result = new ArrayList<>();
        IQueue<QueuedTaskVO> queue = hazelcastInstance.getQueue(queueName);
        QueuedTaskVO[] queueItems = queue.toArray(new QueuedTaskVO[queue.size()]);

        if (queueItems.length > 0) {
            for (int i = (pageNum - 1) * pageSize; i < ((pageSize * pageNum >= queueItems.length) ? queueItems.length : pageSize * pageNum); i++) {
                QueuedTaskVO item = queueItems[i];
                QueuedTaskVO qt = new QueuedTaskVO();
                qt.setId(item.getId());
                qt.setInsertTime(item.getInsertTime());
                qt.setStartTime(item.getStartTime());
                result.add(qt);
            }
        }
        return new GenericPage<>(result, pageNum, pageSize, queueItems.length);
    }

    @Override
    public void instanceCreated(InstanceEvent event) {
        if (event.getInstanceType().isQueue()) {//storing all new queues name
            Set<String> queueNames = hazelcastInstance.getSet(queueListName);
            String queueName = ((IQueue) event.getInstance()).getName();
            queueNames.add(queueName);
            logger.debug("Queue [{}] added to cluster", queueName);
        }
    }

    @Override
    public void instanceDestroyed(InstanceEvent event) {
        if (event.getInstanceType().isQueue()) {//removing queues names
            Set<String> queueNames = hazelcastInstance.getSet(queueListName);
            String queueName = ((IQueue) event.getInstance()).getName();
            queueNames.remove(queueName);
            logger.debug("Queue [{}] removed from cluster", queueName);
        }
    }

    @Override
    @Profiled(notNull = true)
    public UUID poll(String actorId, String taskList) {
        IQueue<QueuedTaskVO> queue = hazelcastInstance.getQueue(createQueueName(actorId, taskList));

        UUID taskId = null;
        try {

            QueuedTaskVO queueItem = queue.poll(pollDelay, pollDelayUnit);

            if (queueItem != null) {
                taskId = queueItem.getId();
                checkpointService.addCheckpoint(new Checkpoint(TimeoutType.TASK_POLL_TO_COMMIT, taskId, actorId, System.currentTimeMillis()));
            }

        } catch (InterruptedException e) {
            logger.error("Thread was interrupted at poll, releasing it", e);
        }

        logger.debug("poll() returns taskId [{}]. Queue.size: {}", taskId, queue.size());

        return taskId;

    }

    @Override
    public void pollCommit(String actorId, UUID taskId) {
        checkpointService.removeEntityCheckpoints(taskId, TimeoutType.TASK_SCHEDULE_TO_START);
        checkpointService.removeEntityCheckpoints(taskId, TimeoutType.TASK_POLL_TO_COMMIT);
    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }

        IQueue<QueuedTaskVO> queue = hazelcastInstance.getQueue(createQueueName(actorId, taskList));
        QueuedTaskVO item = new QueuedTaskVO();
        item.setId(taskId);
        item.setStartTime(startTime);
        item.setInsertTime(System.currentTimeMillis());
        item.setTaskList(taskList);
        queue.add(item);

        //Checkpoints for SCHEDULED_TO_START, SCHEDULE_TO_CLOSE timeouts
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.TASK_SCHEDULE_TO_START, taskId, actorId, startTime));
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.TASK_SCHEDULE_TO_CLOSE, taskId, actorId, startTime));
        logger.debug("enqueueItem() actorId [{}], taskId [{}], startTime [{}]; Queue.size: {}", actorId, taskId, startTime, queue.size());
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return null;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    private String createQueueName(String actorId, String taskList) {
        return (taskList == null) ? actorId : actorId + "#" + taskList;
    }

    public void setQueueListName(String queueListName) {
        this.queueListName = queueListName;
    }

}
