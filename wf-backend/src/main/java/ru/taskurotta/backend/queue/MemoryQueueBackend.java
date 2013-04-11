package ru.taskurotta.backend.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.util.ActorDefinition;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:11 PM
 */
public class MemoryQueueBackend implements QueueBackend {

    private final static Logger logger = LoggerFactory.getLogger(MemoryQueueBackend.class);

    private int pollDelay = 60;
    private Map<String, DelayQueue<DelayedTaskElement>> queues = new ConcurrentHashMap<String, DelayQueue<DelayedTaskElement>>();


    public MemoryQueueBackend(int pollDelay) {

        this.pollDelay = pollDelay;
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
    public UUID poll(ActorDefinition actorDefinition) {

        DelayQueue<DelayedTaskElement> queue = getQueue(actorDefinition.getFullName());

        UUID taskId = null;
        try {

            DelayedTaskElement delayedTaskObject = queue.poll(pollDelay, TimeUnit.SECONDS);

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
    public void pollCommit(ActorDefinition actorDefinition, UUID taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, long startTime) {

        DelayQueue<DelayedTaskElement> queue = getQueue(actorId);
        queue.add(new DelayedTaskElement(taskId, startTime));

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

}
