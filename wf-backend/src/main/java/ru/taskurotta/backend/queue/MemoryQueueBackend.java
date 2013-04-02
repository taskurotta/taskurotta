package ru.taskurotta.backend.queue;

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
    }

    @Override
    public UUID poll(ActorDefinition actorDefinition) {

        DelayQueue<DelayedTaskElement> queue = getQueue(actorDefinition.getFullName());

        DelayedTaskElement task = null;
        try {

            DelayedTaskElement delayedTaskObject = queue.poll(pollDelay, TimeUnit.SECONDS);

            if (delayedTaskObject != null) {
                return delayedTaskObject.taskId;
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

        return null;

    }

    @Override
    public void pollCommit(UUID taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void enqueueItem(ActorDefinition actorDefinition, UUID taskId, long startTime) {

        DelayQueue<DelayedTaskElement> queue = getQueue(actorDefinition.getFullName());
        queue.add(new DelayedTaskElement(taskId, startTime));
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

}
