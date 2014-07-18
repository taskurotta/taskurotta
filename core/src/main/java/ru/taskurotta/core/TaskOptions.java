package ru.taskurotta.core;

import ru.taskurotta.internal.core.ArgType;

import java.util.Arrays;

/**
 * Date: 15.04.13 16:24
 */
public class TaskOptions {
    private ArgType[] argTypes;
    private TaskProperties taskProperties;
    private Promise<?>[] promisesWaitFor;

    protected TaskOptions(){}

    public static class Builder {
        private ArgType[] argTypes;
        private TaskProperties taskProperties;
        private Promise<?>[] promisesWaitFor;

        public Builder withSchedulingOptions(TaskProperties taskProperties) {
            this.taskProperties = taskProperties;
            return this;
        }

        public Builder withArgTypes(ArgType[] argTypes) {
            this.argTypes = argTypes;
            return this;
        }

        public Builder withPromisesWaitFor(Promise<?>[] promisesWaitFor) {
            this.promisesWaitFor = promisesWaitFor;
            return this;
        }

        public TaskOptions build() {
            TaskOptions result = new TaskOptions();
            result.argTypes = argTypes;
            result.taskProperties = taskProperties;
            result.promisesWaitFor = promisesWaitFor;
            return result;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    public ArgType[] getArgTypes() {
        return argTypes;
    }

    public TaskProperties getTaskProperties() {
        return taskProperties;
    }

    public Promise<?>[] getPromisesWaitFor() {
        return promisesWaitFor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskOptions that = (TaskOptions) o;

        if (taskProperties != null ? !taskProperties.equals(that.taskProperties) : that.taskProperties != null)
            return false;
        if (!Arrays.equals(argTypes, that.argTypes)) return false;
        if (!Arrays.equals(promisesWaitFor, that.promisesWaitFor)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = argTypes != null ? Arrays.hashCode(argTypes) : 0;
        result = 31 * result + (taskProperties != null ? taskProperties.hashCode() : 0);
        result = 31 * result + (promisesWaitFor != null ? Arrays.hashCode(promisesWaitFor) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskOptions{" +
                "argTypes=" + Arrays.toString(argTypes) +
                ", actorSchedulingOptions=" + taskProperties +
                ", promisesWaitFor=" + Arrays.toString(promisesWaitFor) +
                '}';
    }
}
