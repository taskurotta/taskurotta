package ru.taskurotta.transport.model;

import ru.taskurotta.internal.core.ArgType;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by void 22.03.13 16:42
 */
public class TaskOptionsContainer implements Serializable {

    private ArgType[] argTypes;
    private TaskConfigContainer taskConfigContainer;
    private ArgContainer[] promisesWaitFor;

    public TaskOptionsContainer() {
    }

    public TaskOptionsContainer(ArgType[] argTypes) {
        this.argTypes = argTypes;
    }

    public TaskOptionsContainer(ArgType[] argTypes, TaskConfigContainer taskConfigContainer, ArgContainer[] promisesWaitFor) {
        this.argTypes = argTypes;
        this.taskConfigContainer = taskConfigContainer;
        this.promisesWaitFor = promisesWaitFor;
    }

    public ArgType[] getArgTypes() {
        return argTypes;
    }

    public TaskConfigContainer getTaskConfigContainer() {
        return taskConfigContainer;
    }

    public ArgContainer[] getPromisesWaitFor() {
        return promisesWaitFor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskOptionsContainer that = (TaskOptionsContainer) o;

        if (taskConfigContainer != null ? !taskConfigContainer.equals(that.taskConfigContainer) : that.taskConfigContainer != null)
            return false;
        if (!Arrays.equals(argTypes, that.argTypes)) return false;
        if (!Arrays.equals(promisesWaitFor, that.promisesWaitFor)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = argTypes != null ? Arrays.hashCode(argTypes) : 0;
        result = 31 * result + (taskConfigContainer != null ? taskConfigContainer.hashCode() : 0);
        result = 31 * result + (promisesWaitFor != null ? Arrays.hashCode(promisesWaitFor) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskOptionsContainer{" +
                "argTypes=" + Arrays.toString(argTypes) +
                ", taskConfigContainer=" + taskConfigContainer +
                ", promisesWaitFor=" + Arrays.toString(promisesWaitFor) +
                "}";
    }
}
