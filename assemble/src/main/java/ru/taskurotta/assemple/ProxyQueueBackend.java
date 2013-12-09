package ru.taskurotta.assemple;

import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;

import java.util.UUID;


public class ProxyQueueBackend implements QueueBackend {

    private final QueueBackend target;

    public ProxyQueueBackend(QueueBackend target) {
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
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        return target.isTaskInQueue(actorId, taskList, taskId, processId);
    }

    @Override
    public String createQueueName(String actorId, String taskList) {
        return target.createQueueName(actorId, taskList);
    }
}
