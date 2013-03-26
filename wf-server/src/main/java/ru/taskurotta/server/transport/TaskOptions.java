package ru.taskurotta.server.transport;

import ru.taskurotta.core.ArgType;

/**
 * Created by void 22.03.13 16:42
 */
public class TaskOptions {
	private ArgType[] argTypes;

	public TaskOptions(ArgType[] argTypes) {
		this.argTypes = argTypes;
	}

	public ArgType[] getArgTypes() {
		return argTypes;
	}
}
