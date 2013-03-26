package ru.taskurotta.internal.core;

import ru.taskurotta.core.ArgType;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;

/**
 * Created by void 22.03.13 13:26
 */
public class MethodDescriptor {
	private TaskTarget taskTarget;
	private ArgType[] argTypes;

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

	public TaskTarget getTaskTarget() {
		return taskTarget;
	}

	public ArgType[] getArgTypes() {
		return argTypes;
	}
}
