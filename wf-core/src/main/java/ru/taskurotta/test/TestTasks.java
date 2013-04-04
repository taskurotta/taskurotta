package ru.taskurotta.test;

import java.util.UUID;

import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskImpl;

/**
 * User: romario
 * Date: 3/29/13
 * Time: 2:59 PM
 */
public class TestTasks {

    public static Task newInstance(UUID taskId, TaskTarget taskTarget, Object[] args) {
        return new TaskImpl(taskId, taskTarget, 0, 0, args, null);
    }

    public static Task newInstance(TaskTarget taskTarget, Object[] args) {
        return new TaskImpl(UUID.randomUUID(), taskTarget, 0, 0, args, null);
    }

    public static Task newInstance(UUID taskId, TaskTarget taskTarget, Object[] args, TaskOptions taskOptions) {
        return new TaskImpl(taskId, taskTarget, 0, 0, args, taskOptions);
    }

    public static Task newInstance(TaskTarget taskTarget, Object[] args, TaskOptions taskOptions) {
        return new TaskImpl(UUID.randomUUID(), taskTarget, 0, 0, args, taskOptions);
    }

    public static Task newInstance(UUID taskId, TaskTarget taskTarget, long startTime, int numberOfAttempts,
                                   Object[] args, TaskOptions taskOptions) {
        return new TaskImpl(taskId, taskTarget, startTime, numberOfAttempts, args, taskOptions);
    }

}
