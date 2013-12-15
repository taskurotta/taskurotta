package ru.taskurotta.service.console.retriever.command;

/**
 * User: dimadin
 * Date: 10.09.13 17:43
 */
public class TaskSearchCommand {

    private String taskId;
    private String processId;

    public TaskSearchCommand() {}

    public TaskSearchCommand(String processId, String taskId) {
        this.processId = processId;
        this.taskId = taskId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public boolean isEmpty() {
        return (taskId == null || taskId.trim().length() == 0) && (processId == null || processId.trim().length() == 0);
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String toString() {
        return "TaskSearchCommand{" +
                "taskId='" + taskId + '\'' +
                ", processId='" + processId + '\'' +
                "} ";
    }
}
