package ru.taskurotta.internal.core;

import ru.taskurotta.core.ActorSchedulingOptions;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.TaskOptions;

import java.util.Arrays;

/**
 * Created by void 26.03.13 10:03
 */
public class TaskOptionsImpl implements TaskOptions {

	private ArgType[] argTypes;
    private ActorSchedulingOptions actorSchedulingOptions;
    private Promise<?>[] promisesWaitFor;

    public TaskOptionsImpl(){

    }

	public TaskOptionsImpl(ArgType[] argTypes) {
		this.argTypes = argTypes;
	}

    public TaskOptionsImpl(ArgType[] argTypes, ActorSchedulingOptions actorSchedulingOptions, Promise<?>[] promisesWaitFor) {
        this.argTypes = argTypes;
        this.actorSchedulingOptions = actorSchedulingOptions;
        this.promisesWaitFor = promisesWaitFor;
    }

    @Override
    public ArgType[] getArgTypes() {
		return argTypes;
	}

    @Override
    public ActorSchedulingOptions getActorSchedulingOptions() {
        return actorSchedulingOptions;
    }

    @Override
    public Promise<?>[] getPromisesWaitFor() {
        return promisesWaitFor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskOptionsImpl that = (TaskOptionsImpl) o;

        if (actorSchedulingOptions != null ? !actorSchedulingOptions.equals(that.actorSchedulingOptions) : that.actorSchedulingOptions != null)
            return false;
        if (!Arrays.equals(argTypes, that.argTypes)) return false;
        if (!Arrays.equals(promisesWaitFor, that.promisesWaitFor)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = argTypes != null ? Arrays.hashCode(argTypes) : 0;
        result = 31 * result + (actorSchedulingOptions != null ? actorSchedulingOptions.hashCode() : 0);
        result = 31 * result + (promisesWaitFor != null ? Arrays.hashCode(promisesWaitFor) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskOptionsImpl{" +
                "argTypes=" + Arrays.toString(argTypes) +
                ", actorSchedulingOptions=" + actorSchedulingOptions +
                ", promisesWaitFor=" + Arrays.toString(promisesWaitFor) +
                '}';
    }
}
