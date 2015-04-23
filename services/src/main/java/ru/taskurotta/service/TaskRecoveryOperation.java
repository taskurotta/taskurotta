package ru.taskurotta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.executor.Operation;
import ru.taskurotta.service.storage.InterruptedTasksService;

import java.util.UUID;

/**
 * Created on 23.04.2015.
 */
public class TaskRecoveryOperation implements Operation<InterruptedTasksService> {

    private static final Logger logger = LoggerFactory.getLogger(TaskRecoveryOperation.class);

    private InterruptedTasksService interruptedTasksService;

    private UUID processId;

    private UUID taskId;

    private TaskRecoveryOperation(UUID processId, UUID taskId) {
        this.processId = processId;
        this.taskId = taskId;
    }

    @Override
    public void init (InterruptedTasksService nativePoint) {
        this.interruptedTasksService = nativePoint;
    }

    @Override
    public void run () {
        try {
            interruptedTasksService.restart(processId, taskId);
        } catch (Throwable e) {
            logger.error("Cannot restart task: processId[{}], taskId[{}]", processId, taskId);
        }
    }

    public UUID getProcessId () {
        return processId;
    }

    public UUID getTaskId () {
        return taskId;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskRecoveryOperation that = (TaskRecoveryOperation) o;

        if (processId != null ? !processId.equals(that.processId) : that.processId != null) return false;
        return !(taskId != null ? !taskId.equals(that.taskId) : that.taskId != null);
    }

    @Override
    public int hashCode () {
        int result = 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (taskId != null ? taskId.hashCode() : 0);
        return result;
    }
}
