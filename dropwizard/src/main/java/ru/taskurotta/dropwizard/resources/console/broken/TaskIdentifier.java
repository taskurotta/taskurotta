package ru.taskurotta.dropwizard.resources.console.broken;

import java.io.Serializable;

/**
 * Created on 23.03.2015.
 */
public class TaskIdentifier implements Serializable {

    private String taskId;
    private String processId;

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
