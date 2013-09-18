package ru.taskurotta.backend.recovery;

import ru.taskurotta.backend.checkpoint.CheckpointService;
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

        // if no tasks in queue, than return current time
        if (time == null) {
            return System.currentTimeMillis();
        }

        return time;
    }

    @Override
    public TaskQueueItem poll(String actorId, String taskList) {

        TaskQueueItem taskQueueItem = queueBackend.poll(actorId, taskList);
        Long current = taskQueueItem.getEnqueueTime();

        String queueName = createQueueName(actorId, taskList);

        synchronized (lastPolledTaskEnqueueTimes) {
            Long previous = lastPolledTaskEnqueueTimes.get(queueName);
            if (previous == null) {
                lastPolledTaskEnqueueTimes.put(queueName, current);
            } else {
                if (previous < current) {
                    lastPolledTaskEnqueueTimes.put(queueName, current);
                }
            }
        }

        return taskQueueItem;
    }

    @Override
    public void pollCommit(String actorId, UUID taskId, UUID processId) {}

    @Override
    public void enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {
        queueBackend.enqueueItem(actorId, taskId, processId, startTime, taskList);
    }

    @Override
    public String createQueueName(String actorId, String taskList) {
        return queueBackend.createQueueName(actorId, taskList);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return null;
    }
}
