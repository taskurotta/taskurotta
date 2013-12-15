package ru.taskurotta.service.recovery;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.queue.MemoryQueueService;
import ru.taskurotta.service.queue.TaskQueueItem;

import java.util.Map;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 18.09.13
 * Time: 11:18
 */
public class MemoryQueueServiceStatistics extends AbstractQueueServiceStatistics {

    private MemoryQueueService queueService;

    public MemoryQueueServiceStatistics(MemoryQueueService queueService) {
        super(queueService);

        this.queueService = queueService;
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        return queueService.getQueueList(pageNum, pageSize);
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return queueService.getQueueTaskCount(queueName);
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        return queueService.getQueueContent(queueName, pageNum, pageSize);
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return queueService.getHoveringCount(periodSize);
    }

    @Override
    public GenericPage<QueueStatVO> getQueuesStatsPage(int pageNum, int pageSize, String filter) {
        return queueService.getQueuesStatsPage(pageNum, pageSize, filter);
    }

    @Override
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        return queueService.isTaskInQueue(actorId, taskList, taskId, processId);
    }
}
