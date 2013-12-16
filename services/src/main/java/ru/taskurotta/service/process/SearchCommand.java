package ru.taskurotta.service.process;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:25
 */
public class SearchCommand {

    protected UUID processId;
    protected String startActorId;
    protected String brokenActorId;
    protected long startPeriod = -1;
    protected long endPeriod = -1;
    protected String errorMessage;
    protected String errorClassName;

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public String getStartActorId() {
        return startActorId;
    }

    public void setStartActorId(String startActorId) {
        this.startActorId = startActorId;
    }

    public String getBrokenActorId() {
        return brokenActorId;
    }

    public void setBrokenActorId(String brokenActorId) {
        this.brokenActorId = brokenActorId;
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

    @Override
    public String toString() {
        return "SearchCommand{" +
                "processId=" + processId +
                ", startActorId='" + startActorId + '\'' +
                ", brokenActorId='" + brokenActorId + '\'' +
                ", startPeriod=" + startPeriod +
                ", endPeriod=" + endPeriod +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorClassName='" + errorClassName + '\'' +
                "} " + super.toString();
    }
}
