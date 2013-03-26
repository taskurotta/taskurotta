package ru.taskurotta.internal;

import ru.taskurotta.core.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * User: romario
 * Date: 3/25/13
 * Time: 11:49 PM
 */
public class RuntimeContext {

    private static ThreadLocal<RuntimeContext> currentContext = new ThreadLocal<RuntimeContext>();

    private List<Task> tasks;

    private RuntimeContext() {

    }


    public static RuntimeContext create() {
        RuntimeContext runtimeContext = new RuntimeContext();
        currentContext.set(runtimeContext);
        return runtimeContext;
    }

    public static void remove() {
        currentContext.remove();
    }

    public static RuntimeContext getCurrent() {
        RuntimeContext runtimeContext = currentContext.get();

        if (runtimeContext == null) {
            throw new IllegalStateException("There is no RuntimeContext");
        }

        return currentContext.get();
    }


    public void handle(Task task) {

        if (tasks == null) {
            tasks = new ArrayList<Task>();
        }

        tasks.add(task);
    }


    public Task[] getTasks() {

        if (tasks == null) {
            return null;
        }

        return tasks.toArray(new Task[tasks.size()]);
    }

}
