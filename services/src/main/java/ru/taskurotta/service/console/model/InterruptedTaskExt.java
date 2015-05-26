package ru.taskurotta.service.console.model;

import java.io.Serializable;

/**
 * Created on 25.05.2015.
 */
public class InterruptedTaskExt extends InterruptedTask implements Serializable {

    protected String fullMessage;
    protected String stackTrace;

    public InterruptedTaskExt() {}

    public InterruptedTaskExt(InterruptedTask task, String fullMessage, String stackTrace) {
        this.processId = task.getProcessId();
        this.taskId = task.getTaskId();
        this.actorId = task.getActorId();
        this.starterId = task.getStarterId();
        this.time = task.getTime();
        this.errorMessage = task.getErrorMessage();
        this.errorClassName = task.getErrorClassName();

        this.fullMessage = fullMessage;
        this.stackTrace = stackTrace;
    }

    public String getFullMessage() {
        return fullMessage;
    }

    public void setFullMessage(String fullMessage) {
        this.fullMessage = fullMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public String toString() {
        return "InterruptedTaskExt{" +
                "fullMessage='" + fullMessage + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                "} " + super.toString();
    }
}
