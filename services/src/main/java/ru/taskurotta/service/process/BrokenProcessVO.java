package ru.taskurotta.service.process;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:24
 */
public class BrokenProcessVO {

    private UUID processId;
    private String startActorId;
    private String brokenActorId;
    private long time;
    private String errorMessage;
    private String errorClassName;
    private String stackTrace;

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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
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

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public String toString() {
        return "BrokenProcessVO{" +
                "processId=" + processId +
                ", startActorId='" + startActorId + '\'' +
                ", brokenActorId='" + brokenActorId + '\'' +
                ", time=" + time +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorClassName='" + errorClassName + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                "} " + super.toString();
    }
}
