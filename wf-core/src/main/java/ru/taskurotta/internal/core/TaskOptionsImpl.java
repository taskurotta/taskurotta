package ru.taskurotta.internal.core;

import ru.taskurotta.core.ArgType;
import ru.taskurotta.core.TaskOptions;

import java.util.Arrays;

/**
 * Created by void 26.03.13 10:03
 */
public class TaskOptionsImpl implements TaskOptions {

	private ArgType[] argTypes;

	public TaskOptionsImpl(ArgType[] argTypes) {
		this.argTypes = argTypes;
	}

	@Override
    public ArgType[] getArgTypes() {
		return argTypes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TaskOptionsImpl that = (TaskOptionsImpl) o;

		if (!Arrays.equals(argTypes, that.argTypes)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return argTypes != null ? Arrays.hashCode(argTypes) : 0;
	}
}
