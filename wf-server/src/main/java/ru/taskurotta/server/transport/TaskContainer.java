package ru.taskurotta.server.transport;

import ru.taskurotta.core.TaskTarget;

import java.util.Arrays;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 3:05 PM
 */
public class TaskContainer {

    private UUID taskId;
    private TaskTarget target;
    private ArgContainer[] args;

    public TaskContainer(UUID taskId, TaskTarget target, ArgContainer[] args) {
        this.taskId = taskId;
        this.target = target;
        this.args = args;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public TaskTarget getTarget() {
        return target;
    }

    public ArgContainer[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "TaskContainer{" +
                "taskId=" + taskId +
                ", target=" + target +
                ", args=" + (args == null ? null : Arrays.asList(args)) +
                '}';
    }
}
