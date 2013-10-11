package ru.taskurotta.backend.process;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:24
 */
public class BrokenProcessVO {

    private String processId;
    private String deciderActorId;
    private String brokenActorId;
    private long time;
    private String errorMessage;
    private String errorClassName;
    private String stackTrace;

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
                "processId='" + processId + '\'' +
                ", deciderActorId='" + deciderActorId + '\'' +
                ", brokenActorId='" + brokenActorId + '\'' +
                ", time=" + time +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorClassName='" + errorClassName + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                "} " + super.toString();
    }
}
