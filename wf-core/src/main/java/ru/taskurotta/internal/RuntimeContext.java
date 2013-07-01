package ru.taskurotta.internal;

import ru.taskurotta.core.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 3/25/13
 * Time: 11:49 PM
 */
public class RuntimeContext {

    private static ThreadLocal<RuntimeContext> currentContext = new ThreadLocal<>();

    private List<Task> tasks;
    private UUID processId;
    private long startTime;

    public RuntimeContext(UUID processId) {
        this.processId = processId;
        this.startTime = System.currentTimeMillis();
    }


    public static RuntimeContext start(UUID processId) {
        RuntimeContext runtimeContext = new RuntimeContext(processId);
        currentContext.set(runtimeContext);
        return runtimeContext;
    }

    public static void finish() {
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
            tasks = new ArrayList<>();
        }

        tasks.add(task);
    }


    public Task[] getTasks() {

        if (tasks == null) {
            return null;
        }

        return tasks.toArray(new Task[tasks.size()]);
    }

    public UUID getProcessId() {
        return processId;
    }

    public long getStartTime() {
        return startTime;
    }
}
