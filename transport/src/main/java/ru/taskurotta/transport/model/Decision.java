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
    long recoveryTime = 0;

    private DecisionContainer decisionContainer;

    public Decision(UUID taskId, UUID processId, int state, UUID pass, long recoveryTime, DecisionContainer
            decisionContainer) {
        this.taskId = taskId;
        this.processId = processId;
        this.state = state;
        this.pass = pass;
        this.recoveryTime = recoveryTime;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Decision decision = (Decision) o;

        if (recoveryTime != decision.recoveryTime) return false;
        if (state != decision.state) return false;
        if (decisionContainer != null ? !decisionContainer.equals(decision.decisionContainer) : decision.decisionContainer != null)
            return false;
        if (pass != null ? !pass.equals(decision.pass) : decision.pass != null) return false;
        if (processId != null ? !processId.equals(decision.processId) : decision.processId != null) return false;
        if (taskId != null ? !taskId.equals(decision.taskId) : decision.taskId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId != null ? taskId.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + state;
        result = 31 * result + (pass != null ? pass.hashCode() : 0);
        result = 31 * result + (int) (recoveryTime ^ (recoveryTime >>> 32));
        result = 31 * result + (decisionContainer != null ? decisionContainer.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Decision{" +
                "taskId=" + taskId +
                ", processId=" + processId +
                ", state=" + state +
                ", pass=" + pass +
                ", recoveryTime=" + recoveryTime +
                ", decisionContainer=" + decisionContainer +
                '}';
    }
}
