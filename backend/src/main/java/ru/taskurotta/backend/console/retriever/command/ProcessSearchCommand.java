package ru.taskurotta.backend.console.retriever.command;

/**
 * User: dimadin
 * Date: 10.09.13 14:12
 */
public class ProcessSearchCommand {

    private String customId;
    private String processId;

    public ProcessSearchCommand() {}

    public ProcessSearchCommand(String processId, String customId) {
       this.processId = processId;
       this.customId = customId;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public boolean isEmpty() {
        return (customId == null || customId.trim().length() == 0) && (processId == null || processId.trim().length() == 0);
    }

    @Override
    public String toString() {
        return "ProcessSearchCommand{" +
                "customId='" + customId + '\'' +
                ", processId='" + processId + '\'' +
                "} " + super.toString();
    }
}
