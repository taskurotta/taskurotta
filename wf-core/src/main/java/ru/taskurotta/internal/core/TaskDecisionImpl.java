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
	private Object value;
    private Task[] tasks;

	public TaskDecisionImpl(UUID uuid, Object value, Task[] tasks) {
        this.uuid = uuid;
        this.value = value;
        this.tasks = tasks;
	}

    @Override
    public UUID getId() {
        return uuid;
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
        if (o == null || getClass() != o.getClass()) return false;

        TaskDecisionImpl that = (TaskDecisionImpl) o;

        if (!Arrays.equals(tasks, that.tasks)) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (tasks != null ? Arrays.hashCode(tasks) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskDecisionImpl{" +
                "uuid=" + uuid +
                ", value=" + value +
                ", tasks=" + (tasks == null ? null : Arrays.asList(tasks)) +
                '}';
    }
}
