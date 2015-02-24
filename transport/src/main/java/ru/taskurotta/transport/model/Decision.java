package ru.taskurotta.transport.model;

import java.util.UUID;

/**
 */
public class Decision {

    public static final int STATE_REGISTERED = 0;
    public static final int STATE_WORK = 1;
    public static final int STATE_FINISH = 2;

    public static class Timeouts {
        long workerTimeout = 0;
        boolean failOnWorkerTimeout = false;

        public Timeouts(long workerTimeout, boolean failOnWorkerTimeout) {
            this.workerTimeout = workerTimeout;
            this.failOnWorkerTimeout = failOnWorkerTimeout;
        }

        public long getWorkerTimeout() {
            return workerTimeout;
        }

        public boolean isFailOnWorkerTimeout() {
            return failOnWorkerTimeout;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Timeouts timeouts = (Timeouts) o;

            if (failOnWorkerTimeout != timeouts.failOnWorkerTimeout) return false;
            if (workerTimeout != timeouts.workerTimeout) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (workerTimeout ^ (workerTimeout >>> 32));
            result = 31 * result + (failOnWorkerTimeout ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Timeouts{" +
                    "workerTimeout=" + workerTimeout +
                    ", failOnWorkerTimeout=" + failOnWorkerTimeout +
                    '}';
        }
    }

    private UUID taskId;
    private UUID processId;
    private int state = STATE_REGISTERED;
    private UUID pass;
    private Timeouts timeouts;

    private DecisionContainer decisionContainer;

    public Decision(UUID taskId, UUID processId, int state, UUID pass, Timeouts timeouts, DecisionContainer
            decisionContainer) {
        this.taskId = taskId;
        this.processId = processId;
        this.state = state;
        this.pass = pass;
        this.timeouts = timeouts;
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

    public Timeouts getTimeouts() {
        return timeouts;
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

    public void setDecisionContainer(DecisionContainer decisionContainer) {
        this.decisionContainer = decisionContainer;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Decision decision = (Decision) o;

        if (state != decision.state) return false;
        if (decisionContainer != null ? !decisionContainer.equals(decision.decisionContainer) : decision.decisionContainer != null)
            return false;
        if (pass != null ? !pass.equals(decision.pass) : decision.pass != null) return false;
        if (processId != null ? !processId.equals(decision.processId) : decision.processId != null) return false;
        if (taskId != null ? !taskId.equals(decision.taskId) : decision.taskId != null) return false;
        if (timeouts != null ? !timeouts.equals(decision.timeouts) : decision.timeouts != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskId != null ? taskId.hashCode() : 0;
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + state;
        result = 31 * result + (pass != null ? pass.hashCode() : 0);
        result = 31 * result + (timeouts != null ? timeouts.hashCode() : 0);
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
                ", timeouts=" + timeouts +
                ", decisionContainer=" + decisionContainer +
                '}';
    }
}
