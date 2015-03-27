package ru.taskurotta.service.console.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created on 23.03.2015.
 */
public class TaskIdentifier implements Serializable {

    private String taskId;
    private String processId;

    public TaskIdentifier(){}

    public TaskIdentifier(UUID taskId, UUID processId) {
        if (taskId != null) {
            this.taskId = taskId.toString();
        }
        if (processId != null) {
            this.processId = processId.toString();
        }
    }

    public TaskIdentifier(String taskId, String processId) {
        this.taskId = taskId;
        this.processId = processId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    @Override
    public String toString() {
        return "TaskIdentifier{" +
                "taskId='" + taskId + '\'' +
                ", processId='" + processId + '\'' +
                '}';
    }
}
