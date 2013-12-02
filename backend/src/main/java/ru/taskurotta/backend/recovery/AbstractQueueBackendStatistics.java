package ru.taskurotta.backend.recovery;

import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: stukushin
 * Date: 18.09.13
 * Time: 12:50
 */
public abstract class AbstractQueueBackendStatistics implements QueueBackendStatistics {

    protected final ConcurrentHashMap<String, Long> lastPolledTaskEnqueueTimes = new ConcurrentHashMap<>();

    private QueueBackend queueBackend;

    protected AbstractQueueBackendStatistics(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
    }

    @Override
    public long getLastPolledTaskEnqueueTime(String queueName) {
        Long time = lastPolledTaskEnqueueTimes.get(queueName);

        // if no tasks in queue, than return -1
        if (time == null) {
            return -1;
        }

        return time;
    }

    @Override
    public TaskQueueItem poll(String actorId, String taskList) {

        TaskQueueItem taskQueueItem = queueBackend.poll(actorId, taskList);

        if (taskQueueItem == null) {
            return taskQueueItem;
        }

        String queueName = createQueueName(actorId, taskList);

        lastPolledTaskEnqueueTimes.put(queueName, taskQueueItem.getEnqueueTime());

        return taskQueueItem;
    }

    @Override
    public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {
        return queueBackend.enqueueItem(actorId, taskId, processId, startTime, taskList);
    }

    @Override
    public String createQueueName(String actorId, String taskList) {
        return queueBackend.createQueueName(actorId, taskList);
    }
}
