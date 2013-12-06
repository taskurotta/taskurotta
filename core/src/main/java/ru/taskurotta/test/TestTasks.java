package ru.taskurotta.test;

import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskImpl;

import java.util.UUID;

/**
 * User: romario
 * Date: 3/29/13
 * Time: 2:59 PM
 */
public class TestTasks {

    public static Task newInstance(UUID taskId, UUID processId, TaskTarget taskTarget, Object[] args) {
        return new TaskImpl(taskId, processId, taskTarget, 0, 0, args, null, null);
    }

    public static Task newInstance(UUID processId, TaskTarget taskTarget, Object[] args) {
        return new TaskImpl(UUID.randomUUID(), processId, taskTarget, 0, 0, args, null, null);
    }

    public static Task newInstance(UUID taskId, UUID processId, TaskTarget taskTarget, Object[] args, TaskOptions taskOptions) {
        return new TaskImpl(taskId, processId, taskTarget, 0, 0, args, taskOptions, null);
    }

    public static Task newInstance(UUID taskId, UUID processId, TaskTarget taskTarget, Object[] args, TaskOptions taskOptions, String[] failTypes) {
        return new TaskImpl(taskId, processId, taskTarget, 0, 0, args, taskOptions, failTypes);
    }

    public static Task newInstance(UUID processId, TaskTarget taskTarget, Object[] args, TaskOptions taskOptions) {
        return new TaskImpl(UUID.randomUUID(), processId, taskTarget, 0, 0, args, taskOptions, null);
    }

    public static Task newInstance(UUID taskId, UUID processId, TaskTarget taskTarget, long startTime, int numberOfAttempts,
                                   Object[] args, TaskOptions taskOptions) {
        return new TaskImpl(taskId, processId, taskTarget, startTime, numberOfAttempts, args, taskOptions, null);
    }

    public static Task newInstance(UUID taskId, UUID processId, TaskTarget taskTarget, long startTime, int numberOfAttempts,
                                   Object[] args, TaskOptions taskOptions, String[] failTypes) {
        return new TaskImpl(taskId, processId, taskTarget, startTime, numberOfAttempts, args, taskOptions, failTypes);
    }

}
