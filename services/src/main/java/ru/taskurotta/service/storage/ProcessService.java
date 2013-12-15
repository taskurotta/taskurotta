package ru.taskurotta.service.storage;

import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 7:58 PM
 */
public interface ProcessService {

    public void startProcess(TaskContainer task);

    public void finishProcess(UUID processId, String returnValue);

    public void deleteProcess(UUID processId);

    /**
     * Get start task for process by it's id
     *
     * @param processId
     */
    public TaskContainer getStartTask(UUID processId);
}
