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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskIdentifier that = (TaskIdentifier) o;

        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        return processId != null ? processId.equals(that.processId) : that.processId == null;

    }

    @Override
    public int hashCode() {
        int result = taskId != null ? taskId.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaskIdentifier{" +
                "taskId='" + taskId + '\'' +
                ", processId='" + processId + '\'' +
                '}';
    }
}
