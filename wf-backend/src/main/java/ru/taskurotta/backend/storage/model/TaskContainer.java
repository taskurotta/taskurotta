package ru.taskurotta.backend.storage.model;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
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
    private UUID processId;
    private TaskTarget target;
    private long startTime;
    private int numberOfAttempts;
    private ArgContainer[] args;
	private TaskOptionsContainer options;

    public TaskContainer(UUID taskId, UUID processId, TaskTarget target, long startTime, int numberOfAttempts,
                         ArgContainer[] args,
                         TaskOptionsContainer options) {
        this.taskId = taskId;
        this.processId = processId;
        this.target = target;
        this.startTime = startTime;
        this.numberOfAttempts = numberOfAttempts;
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

    public long getStartTime() {
        return startTime;
    }

    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }

	public TaskOptionsContainer getOptions() {
		return options;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskContainer)) return false;

        TaskContainer that = (TaskContainer) o;

        if (numberOfAttempts != that.numberOfAttempts) return false;
        if (startTime != that.startTime) return false;
        if (!Arrays.equals(args, that.args)) return false;
        if (options != null ? !options.equals(that.options) : that.options != null) return false;
        if (!processId.equals(that.processId)) return false;
        if (!target.equals(that.target)) return false;
        if (!taskId.equals(that.taskId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId.hashCode();
        result = 31 * result + processId.hashCode();
        result = 31 * result + target.hashCode();
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + numberOfAttempts;
        result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskContainer{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", target=" + target +
                ", startTime=" + startTime +
                ", numberOfAttempts=" + numberOfAttempts +
                ", args=" + (args == null ? null : Arrays.asList(args)) +
                ", options=" + options +
                '}';
    }

    public UUID getProcessId() {
        return processId;
    }
}
