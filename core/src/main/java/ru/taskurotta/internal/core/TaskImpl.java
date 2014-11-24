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

    private UUID id;
    private UUID processId;
    private TaskTarget target;
    private long startTime;
    private int errorAttempts = 0;
    private Object[] args;
	private TaskOptions taskOptions;
    private boolean unsafe;
    private String[] failTypes;

    public TaskImpl(){}

    public TaskImpl(UUID uuid, UUID processId, TaskTarget taskTarget, long startTime, int errorAttempts,
                    Object[] args, TaskOptions taskOptions, boolean unsafe, String[] failTypes) {
        this.processId = processId;

        if (uuid == null) {
            throw new IllegalArgumentException("id can not be null!");
        }

        this.id = uuid;

        if (taskTarget == null) {
            throw new IllegalArgumentException("target can not be null!");
        }

        this.target = taskTarget;
        this.startTime = startTime;
        this.errorAttempts = errorAttempts;

        this.args = args;

		this.taskOptions = taskOptions;
        this.unsafe = unsafe;
        this.failTypes = failTypes;
    }


    @Override
    public UUID getId() {
        return id;
    }

    public UUID getProcessId() {
        return processId;
    }


    @Override
    public TaskTarget getTarget() {
        return target;
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
    public int getErrorAttempts() {
        return errorAttempts;
    }

    public TaskOptions getTaskOptions() {
		return taskOptions;
	}

    public boolean isUnsafe() {
        return unsafe;
    }

    public String[] getFailTypes() {
        return failTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskImpl)) return false;

        TaskImpl task = (TaskImpl) o;

        if (errorAttempts != task.errorAttempts) return false;
        if (startTime != task.startTime) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(args, task.args)) return false;
        if (!processId.equals(task.processId)) return false;
        if (taskOptions != null ? !taskOptions.equals(task.taskOptions) : task.taskOptions != null) return false;
        if (!target.equals(task.target)) return false;
        if (!id.equals(task.id)) return false;
        if (unsafe != task.unsafe) return false;
        return Arrays.equals(failTypes, task.failTypes);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + processId.hashCode();
        result = 31 * result + target.hashCode();
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + errorAttempts;
        result = 31 * result + Arrays.hashCode(args);
        result = 31 * result + (taskOptions != null ? taskOptions.hashCode() : 0);
        if (unsafe) {
            result = 31 * result + Arrays.hashCode(failTypes);
        }
        return result;
    }

    @Override
    public String toString() {
        return "TaskImpl{" +
                "id=" + id +
                ", processId=" + processId +
                ", target=" + target +
                ", startTime=" + startTime +
                ", errorAttempts=" + errorAttempts +
                ", args=" + Arrays.toString(args) +
                ", taskOptions=" + taskOptions +
                ", unsafe=" + unsafe +
                ", failTypes=" + Arrays.toString(failTypes) +
                '}';
    }
}
