package ru.taskurotta.core;

import ru.taskurotta.internal.core.ArgType;

import java.util.Arrays;

/**
 * Date: 15.04.13 16:24
 */
public class TaskOptions {
    private ArgType[] argTypes;
    private TaskConfig taskConfig;
    private Promise<?>[] promisesWaitFor;

    public TaskOptions(){}

    public ArgType[] getArgTypes() {
        return argTypes;
    }

    public TaskConfig getTaskConfig() {
        return taskConfig;
    }

    public Promise<?>[] getPromisesWaitFor() {
        return promisesWaitFor;
    }

    public TaskOptions setArgTypes(ArgType[] argTypes) {
        this.argTypes = argTypes;
        return this;
    }

    public TaskOptions setTaskConfig(TaskConfig taskConfig) {
        this.taskConfig = taskConfig;
        return this;
    }

    public TaskOptions setPromisesWaitFor(Promise<?>[] promisesWaitFor) {
        this.promisesWaitFor = promisesWaitFor;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskOptions that = (TaskOptions) o;

        if (taskConfig != null ? !taskConfig.equals(that.taskConfig) : that.taskConfig != null)
            return false;
        if (!Arrays.equals(argTypes, that.argTypes)) return false;
        if (!Arrays.equals(promisesWaitFor, that.promisesWaitFor)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = argTypes != null ? Arrays.hashCode(argTypes) : 0;
        result = 31 * result + (taskConfig != null ? taskConfig.hashCode() : 0);
        result = 31 * result + (promisesWaitFor != null ? Arrays.hashCode(promisesWaitFor) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskOptions{" +
                "argTypes=" + Arrays.toString(argTypes) +
                ", actorSchedulingOptions=" + taskConfig +
                ", promisesWaitFor=" + Arrays.toString(promisesWaitFor) +
                '}';
    }
}
