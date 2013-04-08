package ru.taskurotta.internal.core;

import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.Arrays;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 28.12.12
 * Time: 16:40
 */
public class TaskDecisionImpl implements TaskDecision {


    private UUID uuid;
    private UUID processId;
	private Object value;
    private Task[] tasks;

	public TaskDecisionImpl(UUID uuid, UUID processId, Object value, Task[] tasks) {
        this.uuid = uuid;
        this.processId = processId;
        this.value = value;
        this.tasks = tasks;
	}

    @Override
    public UUID getId() {
        return uuid;
    }

    @Override
    public UUID getProcessId() {
        return processId;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Task[] getTasks() {
        return tasks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskDecisionImpl)) return false;

        TaskDecisionImpl that = (TaskDecisionImpl) o;

        if (!processId.equals(that.processId)) return false;
        if (!Arrays.equals(tasks, that.tasks)) return false;
        if (!uuid.equals(that.uuid)) return false;
        if (!value.equals(that.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + processId.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (tasks != null ? Arrays.hashCode(tasks) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskDecisionImpl{" +
                "uuid=" + uuid +
                ", processId=" + processId +
                ", value=" + value +
                ", tasks=" + (tasks == null ? null : Arrays.asList(tasks)) +
                '}';
    }
}
