package ru.taskurotta.assemble;

import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.queue.TaskQueueItem;

import java.util.Collection;
import java.util.UUID;

public class ProxyQueueService implements QueueService {

    private final QueueService target;

    public ProxyQueueService(QueueService target) {
        this.target = target;
    }

    @Override
    public TaskQueueItem poll(String actorId, String taskList) {
        return target.poll(actorId, taskList);
    }

    @Override
    public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {
        return target.enqueueItem(actorId, taskId, processId, startTime, taskList);
    }

    @Override
    public String createQueueName(String actorId, String taskList) {
        return target.createQueueName(actorId, taskList);
    }

    @Override
    public long getLastPolledTaskEnqueueTime(String queueName) {
        return target.getLastPolledTaskEnqueueTime(queueName);
    }

    @Override
    public Collection<String> getQueueNames() {
        return target.getQueueNames();
    }
}
