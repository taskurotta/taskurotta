package ru.taskurotta.backend.storage;

import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 7:58 PM
 */
public interface ProcessBackend {

    public void startProcess(TaskContainer task);

    public ProcessVO getProcess(UUID processId);

    public long finishProcess(UUID processId, String returnValue);

    public void deleteProcess(UUID processId);

    /**
     * Get start task for process by it's id
     *
     * @param processId
     */
    public TaskContainer getStartTask(UUID processId);
}
