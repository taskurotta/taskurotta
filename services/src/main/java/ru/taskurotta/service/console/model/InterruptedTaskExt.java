package ru.taskurotta.service.console.model;

import java.io.Serializable;

/**
 * Created on 25.05.2015.
 */
public class InterruptedTaskExt extends InterruptedTask implements Serializable {

    private String fullMessage;
    private String stackTrace;
    private InterruptedTaskType type;

    public enum InterruptedTaskType {
        NONE(0), KNOWN(1), UNKNOWN(2);

        private int type;

        InterruptedTaskType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }

    public InterruptedTaskExt() {}

    public InterruptedTaskExt(InterruptedTask task, String fullMessage, String stackTrace) {
        this(task, fullMessage, stackTrace, InterruptedTaskType.NONE);
    }

    public InterruptedTaskExt(InterruptedTask task, String fullMessage, String stackTrace, InterruptedTaskType type) {
        this.processId = task.getProcessId();
        this.taskId = task.getTaskId();
        this.actorId = task.getActorId();
        this.starterId = task.getStarterId();
        this.time = task.getTime();
        this.errorMessage = task.getErrorMessage();
        this.errorClassName = task.getErrorClassName();

        this.fullMessage = fullMessage;
        this.stackTrace = stackTrace;
        this.type = type;
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

    public InterruptedTaskType getType() {
        return type;
    }

    public void setType(InterruptedTaskType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "InterruptedTaskExt{" +
                "fullMessage='" + fullMessage + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                ", type=" + type +
                '}';
    }
}
