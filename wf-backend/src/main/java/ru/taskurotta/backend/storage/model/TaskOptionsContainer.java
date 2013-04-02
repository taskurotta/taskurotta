package ru.taskurotta.backend.storage.model;

import ru.taskurotta.core.ArgType;

import java.util.Arrays;

/**
 * Created by void 22.03.13 16:42
 */
public class TaskOptionsContainer {

	private ArgType[] argTypes;

	public TaskOptionsContainer(ArgType[] argTypes) {
		this.argTypes = argTypes;
	}

	public ArgType[] getArgTypes() {
		return argTypes;
	}


    @Override
    public String toString() {
        return "TaskOptionsContainer{" +
                "argTypes=" + (argTypes == null ? null : Arrays.asList(argTypes)) +
                '}';
    }
}
