package ru.taskurotta.backend.hz.queue;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.InstanceEvent;
import com.hazelcast.core.InstanceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.QueuedTaskVO;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.hz.support.HzPartitionResolver;
import ru.taskurotta.backend.hz.support.PartitionResolver;
import ru.taskurotta.backend.queue.QueueBackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by void, dudin 07.06.13 11:00
 */
public class HazelcastQueueBackend implements QueueBackend, QueueInfoRetriever, InstanceListener {

    private final static Logger logger = LoggerFactory.getLogger(HazelcastQueueBackend.class);

    private int pollDelay = 60;
    private TimeUnit pollDelayUnit = TimeUnit.SECONDS;
    private CheckpointService checkpointService = new MemoryCheckpointService();

    //Hazelcast specific
    private HazelcastInstance hazelcastInstance;
    private String queueListName = "tsQueuesList";
    private PartitionResolver partitionResolver;

    public HazelcastQueueBackend(int pollDelay, TimeUnit pollDelayUnit, HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        this.pollDelay = pollDelay;
        this.pollDelayUnit = pollDelayUnit;

        this.hazelcastInstance.addInstanceListener(this);
        logger.debug("HazelcastQueueBackend created and registered as Hazelcast instance listener");
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        List<String> result = new ArrayList<>();
        Set<String> queueNamesSet = hazelcastInstance.getSet(queueListName);
        String[] queueNames = queueNamesSet.toArray(new String[queueNamesSet.size()]);
        if (queueNames!=null && queueNames.length>0) {
            for (int i = (pageNum - 1) * pageSize; i <= ((pageSize * pageNum >= (queueNames.length)) ? (queueNames.length) - 1 : pageSize * pageNum - 1); i++) {
                result.add(queueNames[i]);
            }
        }
        return new GenericPage<String>(result, pageNum, pageSize, queueNames.length);
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return hazelcastInstance.getQueue(queueName).size();
    }

    @Override
    public GenericPage<QueuedTaskVO> getQueueContent(String queueName, int pageNum, int pageSize) {
        List<QueuedTaskVO> result = new ArrayList<QueuedTaskVO>();
        IQueue<PartitionedQueuedTaskVO> queue = hazelcastInstance.getQueue(queueName);
        PartitionedQueuedTaskVO[] queueItems = queue.toArray(new PartitionedQueuedTaskVO[queue.size()]);

        if (queueItems.length > 0) {
            for (int i = (pageNum - 1) * pageSize; i <= ((pageSize * pageNum >= (queueItems.length)) ? (queueItems.length) - 1 : pageSize * pageNum - 1); i++) {
                PartitionedQueuedTaskVO item = queueItems[i];
                QueuedTaskVO qt = new QueuedTaskVO();
                qt.setId(item.getId());
                qt.setInsertTime(item.getInsertTime());
                qt.setStartTime(item.getStartTime());
                result.add(qt);
            }
        }
        return new GenericPage<QueuedTaskVO>(result, pageNum, pageSize, queueItems.length);
    }

    @Override
    public void instanceCreated(InstanceEvent event) {
        if(event.getInstanceType().isQueue()) {//storing all new queues name
            Set<String> queueNames = hazelcastInstance.getSet(queueListName);
            String queueName = ((IQueue)event.getInstance()).getName();
            queueNames.add(queueName);
            logger.debug("Queue [[]] added to cluster", queueName);
        }
    }

    @Override
    public void instanceDestroyed(InstanceEvent event) {
        if(event.getInstanceType().isQueue()) {//removing queues names
            Set<String> queueNames = hazelcastInstance.getSet(queueListName);
            String queueName = ((IQueue)event.getInstance()).getName();
            queueNames.remove(queueName);
            logger.debug("Queue [[]] removed from cluster", queueName);
        }
    }

    @Override
    public UUID poll(String actorId, String taskList) {
        IQueue<PartitionedQueuedTaskVO> queue = hazelcastInstance.getQueue(createQueueName(actorId, taskList));

        UUID taskId = null;
        try {

            PartitionedQueuedTaskVO queueItem = queue.poll(pollDelay, pollDelayUnit);

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

        Object partitionKey = partitionResolver!=null? partitionResolver.resolveByUUID(processId): 0;

        IQueue<PartitionedQueuedTaskVO> queue = hazelcastInstance.getQueue(createQueueName(actorId, taskList));
        PartitionedQueuedTaskVO item = new PartitionedQueuedTaskVO();
        item.setId(taskId);
        item.setStartTime(startTime);
        item.setPartitionKey(partitionKey);
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

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    private String createQueueName(String actorId, String taskList) {
        return (taskList == null) ? actorId : actorId + "#" + taskList;
    }

    public void setQueueListName(String queueListName) {
        this.queueListName = queueListName;
    }

    public void setPartitionResolver(HzPartitionResolver partitionResolver) {
        this.partitionResolver = partitionResolver;
    }
}
