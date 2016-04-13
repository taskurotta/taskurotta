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

    private static ThreadLocal<RuntimeContext> currentContext = new ThreadLocal<RuntimeContext>();

    private List<Task> tasks;
    private UUID taskId;
    private UUID processId;
    private long startTime;
    private Heartbeat heartbeat;

    public RuntimeContext(UUID taskId, UUID processId, Heartbeat heartbeat) {
        this.taskId = taskId;
        this.processId = processId;
        this.startTime = System.currentTimeMillis();
        this.heartbeat = heartbeat;
    }


    public static RuntimeContext start(UUID taskId, UUID processId, Heartbeat heartbeat) {
        RuntimeContext runtimeContext = new RuntimeContext(taskId, processId, heartbeat);
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

    public UUID getProcessId() {
        return processId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void updateTimeout(long timeout) {
        heartbeat.updateTimeout(timeout);
    }
}
