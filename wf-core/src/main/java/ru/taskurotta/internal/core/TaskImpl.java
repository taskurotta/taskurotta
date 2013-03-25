package ru.taskurotta.internal.core;

import ru.taskurotta.core.ArgType;
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
	private ArgType[] argTypes;


    public TaskImpl(UUID uuid, TaskTarget taskTarget, Object[] args) {
		this(uuid, taskTarget, args, null);
	}

    public TaskImpl(UUID uuid, TaskTarget taskTarget, Object[] args, ArgType[] argTypes) {

        if (uuid == null) {
            throw new IllegalArgumentException("uuid can not be null!");
        }

        this.uuid = uuid;

        if (taskTarget == null) {
            throw new IllegalArgumentException("taskTarget can not be null!");
        }

        this.taskTarget = taskTarget;

        this.args = args;
		this.argTypes = argTypes;
    }


    public TaskImpl(TaskTarget taskTarget, Object[] args, ArgType[] argTypes) {
		this(UUID.randomUUID(), taskTarget, args, argTypes);
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

	public ArgType[] getArgTypes() {
		return argTypes;
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

		if (!Arrays.equals(argTypes, that.getArgTypes())) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + taskTarget.hashCode();
        result = 31 * result + Arrays.deepHashCode(args);
        result = 31 * result + Arrays.deepHashCode(argTypes);

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
