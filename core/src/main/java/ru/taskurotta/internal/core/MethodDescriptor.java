package ru.taskurotta.internal.core;

import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.transport.model.ArgType;
import ru.taskurotta.transport.model.TaskType;

/**
 * Created by void 22.03.13 13:26
 */
public class MethodDescriptor {

    private int positionActorSchedulingOptions = -1;
    private int positionPromisesWaitFor = -1;

	private TaskTarget taskTarget;
	private ArgType[] argTypes;
    private boolean unsafe;
    private String[] failTypes;

	public MethodDescriptor(TaskType type, String name, String version, String method) {
		this(new TaskTargetImpl(type, name, version, method));
	}

	public MethodDescriptor(TaskTarget target) {
		this(target, null);
	}

	public MethodDescriptor(TaskTarget taskTarget, ArgType[] argTypes) {
		this.taskTarget = taskTarget;
		this.argTypes = argTypes;
	}

    public MethodDescriptor(TaskTarget taskTarget, ArgType[] argTypes, int positionActorSchedulingOptions, int positionPromisesWaitFor, boolean unsafe, String[] failTypes) {
        this.taskTarget = taskTarget;
        this.argTypes = argTypes;
        this.positionActorSchedulingOptions = positionActorSchedulingOptions;
        this.positionPromisesWaitFor = positionPromisesWaitFor;
        this.failTypes = failTypes;
        this.unsafe = unsafe;
    }

	public TaskTarget getTaskTarget() {
		return taskTarget;
	}

	public ArgType[] getArgTypes() {
		return argTypes;
	}

    public int getPositionActorSchedulingOptions() {
        return positionActorSchedulingOptions;
    }

    public int getPositionPromisesWaitFor() {
        return positionPromisesWaitFor;
    }

    public boolean isUnsafe() {
        return unsafe;
    }

    public String[] getFailTypes() {
        return failTypes;
    }
}
