package ru.taskurotta.internal.core;

import ru.taskurotta.core.Task;
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
    private TaskTarget taskTarget;
    private Object[] args;


    public TaskImpl(UUID uuid, TaskTarget taskTarget, Object[] args) {

        if (uuid == null) {
            throw new IllegalArgumentException("uuid can not be null!");
        }

        this.uuid = uuid;

        if (taskTarget == null) {
            throw new IllegalArgumentException("taskTarget can not be null!");
        }

        this.taskTarget = taskTarget;

        this.args = args;
    }


    public TaskImpl(TaskTarget taskTarget, Object[] args) {
        this(UUID.randomUUID(), taskTarget, args);
    }


    @Override
    public UUID getId() {
        return uuid;
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
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof Task)) return false;

        Task that = (Task) o;

        if (!uuid.equals(that.getId())) return false;
        if (!taskTarget.equals(that.getTarget())) return false;

        Object[] thatArgs = that.getArgs();

        // if (args == null && thatArgs) we assume that it is empty
        if ((args == null && thatArgs != null)
                || (args != null && (thatArgs == null || !Arrays.deepEquals(args, thatArgs)))) return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + taskTarget.hashCode();
        result = 31 * result + Arrays.deepHashCode(args);

        return result;
    }


    @Override
    public String toString() {
        return "TaskImpl{" +
                "uuid=" + uuid +
                ", taskTarget='" + taskTarget + '\'' +
                ", args=" + (args == null ? "null" : Arrays.toString(args)) +
                "}";
    }

}
