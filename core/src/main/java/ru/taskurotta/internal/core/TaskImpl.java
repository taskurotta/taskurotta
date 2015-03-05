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
    private UUID pass;
    private TaskTarget target;
    private long startTime;
    private int errorAttempts = 0;
    private Object[] args;
	private TaskOptions taskOptions;
    private boolean unsafe;
    private String[] failTypes;

    public TaskImpl(){}

    public TaskImpl(UUID uuid, UUID processId, UUID pass, TaskTarget taskTarget, long startTime, int errorAttempts,
                    Object[] args, TaskOptions taskOptions, boolean unsafe, String[] failTypes) {
        this.processId = processId;

        if (uuid == null) {
            throw new IllegalArgumentException("id can not be null!");
        }

        this.id = uuid;
        this.pass = pass;

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

    @Override
    public TaskOptions getTaskOptions() {
		return taskOptions;
	}

    @Override
    public boolean isUnsafe() {
        return unsafe;
    }

    @Override
    public String[] getFailTypes() {
        return failTypes;
    }

    @Override
    public UUID getPass() {
        return pass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskImpl task = (TaskImpl) o;

        if (errorAttempts != task.errorAttempts) return false;
        if (startTime != task.startTime) return false;
        if (unsafe != task.unsafe) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(args, task.args)) return false;
        if (!Arrays.equals(failTypes, task.failTypes)) return false;
        if (id != null ? !id.equals(task.id) : task.id != null) return false;
        if (pass != null ? !pass.equals(task.pass) : task.pass != null) return false;
        if (processId != null ? !processId.equals(task.processId) : task.processId != null) return false;
        if (target != null ? !target.equals(task.target) : task.target != null) return false;
        if (taskOptions != null ? !taskOptions.equals(task.taskOptions) : task.taskOptions != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (pass != null ? pass.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + errorAttempts;
        result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
        result = 31 * result + (taskOptions != null ? taskOptions.hashCode() : 0);
        result = 31 * result + (unsafe ? 1 : 0);
        result = 31 * result + (failTypes != null ? Arrays.hashCode(failTypes) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskImpl{" +
                "id=" + id +
                ", processId=" + processId +
                ", pass=" + pass +
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
