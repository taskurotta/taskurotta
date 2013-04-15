package ru.taskurotta.backend.queue;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:11 PM
 */
public class MemoryQueueBackend implements QueueBackend {

    private final static Logger logger = LoggerFactory.getLogger(MemoryQueueBackend.class);

    private int pollDelay = 60;
    private TimeUnit pollDelayUnit = TimeUnit.SECONDS;
    private Map<String, DelayQueue<DelayedTaskElement>> queues = new ConcurrentHashMap<String, DelayQueue<DelayedTaskElement>>();
    private CheckpointService checkpointService = new MemoryCheckpointService();

    public MemoryQueueBackend(int pollDelay) {

        this.pollDelay = pollDelay;
    }

    public MemoryQueueBackend(int pollDelay, TimeUnit pollDelayUnit) {

        this.pollDelay = pollDelay;
        this.pollDelayUnit = pollDelayUnit;
    }


    /**
     * Helper class for Delayed queue
     */
    private static class DelayedTaskElement implements Delayed {

        protected UUID taskId;

        protected long startTime;

        public DelayedTaskElement(UUID taskId, long startTime) {

            this.taskId = taskId;
            this.startTime = startTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(startTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.valueOf(((DelayedTaskElement) o).startTime).compareTo(startTime);
        }

        /**
         * startTime not used because we assume than no duplication in queue
         *
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DelayedTaskElement)) return false;

            DelayedTaskElement that = (DelayedTaskElement) o;

            if (!taskId.equals(that.taskId)) return false;

            return true;
        }

        /**
         * startTime not used because we assume than no duplication in queue
         */
        @Override
        public int hashCode() {
            return taskId.hashCode();
        }
    }

    @Override
    public UUID poll(String actorId, String taskList) {

        DelayQueue<DelayedTaskElement> queue = getQueue(actorId);

        UUID taskId = null;
        try {

            DelayedTaskElement delayedTaskObject = queue.poll(pollDelay, pollDelayUnit);

            if (delayedTaskObject != null) {
                taskId = delayedTaskObject.taskId;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();

            // TODO: Where general policy about exceptions ?
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        logger.debug("poll() returns taskId [{}]. Queue.size: {}", taskId, queue.size());

        return taskId;

    }

    @Override
    public void pollCommit(String actorId, UUID taskId) {
        checkpointService.removeEntityCheckpoints(taskId, TimeoutType.TASK_SCHEDULE_TO_START);
    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, long startTime, String taskList) {

        DelayQueue<DelayedTaskElement> queue = getQueue(actorId);
        queue.add(new DelayedTaskElement(taskId, startTime));

        //Checkpoints for SCHEDULED_TO_START, SCHEDULE_TO_CLOSE timeouts
        Checkpoint scheduleToStartCpt = new Checkpoint(TimeoutType.TASK_SCHEDULE_TO_START, taskId, actorId, startTime);
        checkpointService.addCheckpoint(scheduleToStartCpt);
        Checkpoint scheduleToClose = new Checkpoint(TimeoutType.TASK_SCHEDULE_TO_CLOSE, taskId, actorId, startTime);
        checkpointService.addCheckpoint(scheduleToClose);

        logger.debug("enqueueItem() actorId [{}], taskId [{}], startTime [{}]; Queue.size: {}", actorId, taskId, startTime, queue.size());
    }


    private DelayQueue<DelayedTaskElement> getQueue(String queueName) {

        DelayQueue<DelayedTaskElement> queue = queues.get(queueName);
        if (queue == null) {
            synchronized (this) {
                queue = new DelayQueue<DelayedTaskElement>();
                queues.put(queueName, queue);
            }
        }

        return queue;
    }

    public boolean isTaskInQueue(ActorDefinition actorDefinition, UUID taskId) {
        DelayQueue<DelayedTaskElement> queue = getQueue(actorDefinition.getFullName());

        DelayedTaskElement delayedTaskElement = new DelayedTaskElement(taskId, 0);

        return queue.contains(delayedTaskElement);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }


}
