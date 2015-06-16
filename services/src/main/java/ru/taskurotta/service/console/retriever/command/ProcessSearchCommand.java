package ru.taskurotta.service.console.retriever.command;

/**
 * Date: 10.09.13 14:12
 */
public class ProcessSearchCommand extends PageCommand {

    private String customId;
    private String processId;
    private String actorId;
    private int state;
    private long startedFrom;
    private long startedTill;

    public ProcessSearchCommand() {}

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

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getStartedFrom() {
        return startedFrom;
    }

    public void setStartedFrom(long startedFrom) {
        this.startedFrom = startedFrom;
    }

    public long getStartedTill() {
        return startedTill;
    }

    public void setStartedTill(long startedTill) {
        this.startedTill = startedTill;
    }

    public boolean isFilterEmpty() {
        return (customId == null || customId.trim().length() == 0)
                && (processId == null || processId.trim().length() == 0)
                && (actorId==null || actorId.trim().length()==0)
                && startedFrom <= 0l
                && startedTill <= 0l
                && state<0;
    }

    @Override
    public String toString() {
        return "ProcessSearchCommand{" +
                "pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", customId='" + customId + '\'' +
                ", processId='" + processId + '\'' +
                ", actorId='" + actorId + '\'' +
                ", state=" + state +
                ", startedFrom=" + startedFrom +
                ", startedTill=" + startedTill +
                '}';
    }
}
