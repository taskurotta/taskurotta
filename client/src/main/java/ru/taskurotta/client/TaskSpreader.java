package ru.taskurotta.client;

import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.UUID;

/**
 * User: romario
 * Date: 1/29/13
 * Time: 5:44 PM
 */
public interface TaskSpreader {

    /**
     * @return then return polled task or null if queue was empty
     */
    public Task poll();


    /**
     * TODO: TaskDecision should have original task id
     *
     * @param taskDecision
     */
    public void release(TaskDecision taskDecision);

    void updateTimeout(UUID taskId, UUID processId, long timeout);
}
