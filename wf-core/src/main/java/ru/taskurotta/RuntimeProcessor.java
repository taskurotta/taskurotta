package ru.taskurotta;

import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.List;

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
    public TaskDecision execute(Task task);


    /**
     * Record all intercepted tasks
     *
     * @param runnable
     * @return List af intercepted tasks
     */
    public List<Task> execute(Runnable runnable);
}
