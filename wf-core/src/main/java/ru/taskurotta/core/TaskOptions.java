package ru.taskurotta.core;

import java.util.Arrays;

/**
 * Created by void 26.03.13 10:03
 */
public class TaskOptions {

    private ArgType[] argTypes;

    public TaskOptions(ArgType[] argTypes) {
        this.argTypes = argTypes;
    }

    public ArgType[] getArgTypes() {
        return argTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskOptions that = (TaskOptions) o;

        if (!Arrays.equals(argTypes, that.argTypes)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return argTypes != null ? Arrays.hashCode(argTypes) : 0;
    }
}
