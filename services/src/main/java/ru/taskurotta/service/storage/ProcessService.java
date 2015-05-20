package ru.taskurotta.service.storage;

import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

public interface ProcessService {

    void startProcess(TaskContainer task);

    void finishProcess(UUID processId, String returnValue);

    void deleteProcess(UUID processId);

    TaskContainer getStartTask(UUID processId);

    void markProcessAsBroken(UUID processId);

    void markProcessAsStarted(UUID processId);

    void markProcessAsAborted(UUID processId);

    Process getProcess(UUID processUUID);

    ResultSetCursor<UUID> findIncompleteProcesses(long recoveryTime, int limit);

    void lock(UUID processId);

    void unlock(UUID processId);
}
