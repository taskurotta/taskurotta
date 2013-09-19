package ru.taskurotta.backend.recovery;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.queue.MemoryQueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;

import java.util.Map;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 18.09.13
 * Time: 11:18
 */
public class MemoryQueueBackendStatistics extends AbstractQueueBackendStatistics {

    private MemoryQueueBackend queueBackend;

    protected MemoryQueueBackendStatistics(MemoryQueueBackend queueBackend) {
        super(queueBackend);

        this.queueBackend = queueBackend;
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        return queueBackend.getQueueList(pageNum, pageSize);
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return queueBackend.getQueueTaskCount(queueName);
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        return queueBackend.getQueueContent(queueName, pageNum, pageSize);
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return queueBackend.getHoveringCount(periodSize);
    }

    @Override
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        return queueBackend.isTaskInQueue(actorId, taskList, taskId, processId);
    }
}
