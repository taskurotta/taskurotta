package ru.taskurotta;

import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.Heartbeat;

import java.util.UUID;

/**
 * User: romario
 * Date: 1/22/13
 * Time: 4:20 PM
 */
public interface RuntimeProcessor {

    /**
     * Execute task of process
     *
     * @param task
     * @return
     */
    public TaskDecision execute(Task task, Heartbeat heartbeat);


    /**
     * Record all intercepted tasks
     *
     * @param processId
     * @param runnable
     * @return Array af intercepted tasks
     */
    public Task[] execute(UUID taskId, UUID processId,  Heartbeat heartbeat, Runnable runnable);
}
