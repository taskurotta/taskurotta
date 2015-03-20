package ru.taskurotta.service.console.model;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:25
 */
public class SearchCommand {

    protected UUID processId;
    protected UUID taskId;
    protected String actorId;
    protected String starterId;
    protected long startPeriod = -1;
    protected long endPeriod = -1;
    protected String errorMessage;
    protected String errorClassName;

    public UUID getProcessId() {
        return processId;
    }

    public String getStarterId() {
        return starterId;
    }

    public void setStarterId(String starterId) {
        this.starterId = starterId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public long getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(long startPeriod) {
        this.startPeriod = startPeriod;
    }

    public long getEndPeriod() {
        return endPeriod;
    }

    public void setEndPeriod(long endPeriod) {
        this.endPeriod = endPeriod;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorClassName() {
        return errorClassName;
    }

    public void setErrorClassName(String errorClassName) {
        this.errorClassName = errorClassName;
    }
}
