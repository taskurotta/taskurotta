package ru.taskurotta.internal.core;

import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;

import java.util.Arrays;
import java.util.UUID;

/**
 * User: stukushin, romario
 * Date: 28.12.12
 * Time: 16:30
 */
public class TaskImpl implements Task {

    private UUID uuid;
    private UUID processId;
    private TaskTarget taskTarget;
    private long startTime;
    private int numberOfAttempts = 0;
    private Object[] args;
	private TaskOptions taskOptions;


    public TaskImpl(UUID uuid, UUID processId, TaskTarget taskTarget, long startTime, int numberOfAttempts,
                    Object[] args,
                    TaskOptions taskOptions) {
        this.processId = processId;

        if (uuid == null) {
            throw new IllegalArgumentException("uuid can not be null!");
        }

        this.uuid = uuid;

        if (taskTarget == null) {
            throw new IllegalArgumentException("taskTarget can not be null!");
        }

        this.taskTarget = taskTarget;
        this.startTime = startTime;
        this.numberOfAttempts = numberOfAttempts;

        this.args = args;

		if (taskOptions == null) {
			this.taskOptions = new TaskOptionsImpl(null);
		} else {
			this.taskOptions = taskOptions;
		}
    }


    @Override
    public UUID getId() {
        return uuid;
    }

    public UUID getProcessId() {
        return processId;
    }


    @Override
    public TaskTarget getTarget() {
        return taskTarget;
    }


    @Override
    public Object[] getArgs() {
        return args;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }

    public TaskOptions getTaskOptions() {
		return taskOptions;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskImpl)) return false;

        TaskImpl task = (TaskImpl) o;

        if (numberOfAttempts != task.numberOfAttempts) return false;
        if (startTime != task.startTime) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(args, task.args)) return false;
        if (!processId.equals(task.processId)) return false;
        if (taskOptions != null ? !taskOptions.equals(task.taskOptions) : task.taskOptions != null) return false;
        if (!taskTarget.equals(task.taskTarget)) return false;
        if (!uuid.equals(task.uuid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + processId.hashCode();
        result = 31 * result + taskTarget.hashCode();
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + numberOfAttempts;
        result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
        result = 31 * result + (taskOptions != null ? taskOptions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskImpl{" +
                "uuid=" + uuid +
                ", processId=" + processId +
                ", taskTarget=" + taskTarget +
                ", startTime=" + startTime +
                ", numberOfAttempts=" + numberOfAttempts +
                ", args=" + (args == null ? null : Arrays.asList(args)) +
                ", taskOptions=" + taskOptions +
                '}';
    }
}
