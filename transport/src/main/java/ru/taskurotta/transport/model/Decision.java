package ru.taskurotta.transport.model;

import java.util.UUID;

/**
 */
public class Decision {

    public static final int STATE_REGISTERED = 0;
    public static final int STATE_WORK = 1;
    public static final int STATE_FINISH = 2;

    private UUID taskId;
    private UUID processId;
    private int state = STATE_REGISTERED;
    private UUID pass;
    private long recoveryTime = 0;
    private int errorAttempts = 0;

    private DecisionContainer decisionContainer;

    public Decision(UUID taskId, UUID processId, int state, UUID pass, long recoveryTime, int errorAttempts,
                    DecisionContainer decisionContainer) {
        this.taskId = taskId;
        this.processId = processId;
        this.state = state;
        this.pass = pass;
        this.recoveryTime = recoveryTime;
        this.errorAttempts = errorAttempts;
        this.decisionContainer = decisionContainer;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getProcessId() {
        return processId;
    }

    public int getState() {
        return state;
    }

    public UUID getPass() {
        return pass;
    }

    public long getRecoveryTime() {
        return recoveryTime;
    }

    public DecisionContainer getDecisionContainer() {
        return decisionContainer;
    }

    public void setPass(UUID pass) {
        this.pass = pass;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setRecoveryTime(long recoveryTime) {
        this.recoveryTime = recoveryTime;
    }

    public void setDecisionContainer(DecisionContainer decisionContainer) {
        this.decisionContainer = decisionContainer;
    }

    public int getErrorAttempts() {
        return errorAttempts;
    }

    public void setErrorAttempts(int errorAttempts) {
        this.errorAttempts = errorAttempts;
    }

    public void incrementErrorAttempts() {
        this.errorAttempts++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Decision decision = (Decision) o;

        if (state != decision.state) return false;
        if (recoveryTime != decision.recoveryTime) return false;
        if (errorAttempts != decision.errorAttempts) return false;
        if (!taskId.equals(decision.taskId)) return false;
        if (!processId.equals(decision.processId)) return false;
        if (pass != null ? !pass.equals(decision.pass) : decision.pass != null) return false;
        return !(decisionContainer != null ? !decisionContainer.equals(decision.decisionContainer) : decision.decisionContainer != null);

    }

    @Override
    public int hashCode() {
        int result = taskId.hashCode();
        result = 31 * result + processId.hashCode();
        result = 31 * result + state;
        result = 31 * result + (pass != null ? pass.hashCode() : 0);
        result = 31 * result + (int) (recoveryTime ^ (recoveryTime >>> 32));
        result = 31 * result + errorAttempts;
        result = 31 * result + (decisionContainer != null ? decisionContainer.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Decision{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", state=" + Decision.getStateString(state) +
                ", pass=" + pass +
                ", recoveryTime=" + recoveryTime +
                ", errorAttempts=" + errorAttempts +
                ", decisionContainer=" + decisionContainer +
                '}';
    }


    public static String getStateString(int state) {
        switch (state) {
            case 0: return "ready";
            case 1: return "in progress";
            case 2: return "done";
        }

        return "unknown " + state;
    }
}
