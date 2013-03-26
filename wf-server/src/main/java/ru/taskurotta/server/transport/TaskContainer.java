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
	private TaskOptionsContainer options;

    public TaskContainer(UUID taskId, TaskTarget target, ArgContainer[] args) {
		this(taskId, target, args, null);
	}

    public TaskContainer(UUID taskId, TaskTarget target, ArgContainer[] args, TaskOptionsContainer options) {
        this.taskId = taskId;
        this.target = target;
        this.args = args;
		this.options = options;
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

	public TaskOptionsContainer getOptions() {
		return options;
	}

	@Override
	public String toString() {
		return "TaskContainer{" +
				"taskId=" + taskId +
				", target=" + target +
				", args=" + (args == null ? null : Arrays.asList(args)) +
				", options=" + options +
				'}';
	}
}
