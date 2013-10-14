package ru.taskurotta.backend.process;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:25
 */
public class SearchObject {

    private String processId;
    private String deciderActorId;
    private String brokenActorId;
    private long startPeriod = -1;
    private long endPeriod = -1;
    private String errorMessage;
    private String errorClassName;

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getDeciderActorId() {
        return deciderActorId;
    }

    public void setDeciderActorId(String deciderActorId) {
        this.deciderActorId = deciderActorId;
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
}
