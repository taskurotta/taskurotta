package ru.taskurotta.service.storage;

import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

public interface ProcessService {

    public void startProcess(TaskContainer task);

    public void finishProcess(UUID processId, String returnValue);

    public void deleteProcess(UUID processId);

    public TaskContainer getStartTask(UUID processId);

    public void markProcessAsBroken(UUID processId);

    public void markProcessAsStarted(UUID processId);

    public Process getProcess(UUID processUUID);

    public ResultSetCursor<UUID> findProcesses(long recoveryTime, int limit);
}
