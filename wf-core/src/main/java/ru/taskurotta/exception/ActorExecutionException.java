package ru.taskurotta.exception;

/**
 * <code>ActorExecutionException</code> is the superclass of those
 * exceptions that can be thrown during execution of client actors code
 */
public class ActorExecutionException extends Throwable implements Retriable {

    private static final long serialVersionUID = 1718369729072340187L;

    private long restartTime;

    private boolean shouldBeRestarted;

    public ActorExecutionException(String message) {
        super(message);
    }

    public ActorExecutionException(String message, boolean shouldBeRestarted, long restartTime) {
        this(message);
        this.restartTime = restartTime;
        this.shouldBeRestarted = shouldBeRestarted;
    }

    public ActorExecutionException(Throwable cause) {
        super(cause);
    }

    public ActorExecutionException(Throwable cause, boolean shouldBeRestarted, long restartTime) {
        this(cause);
        this.restartTime = restartTime;
        this.shouldBeRestarted = shouldBeRestarted;
    }

    public long getRestartTime() {
        return restartTime;
    }

    public void setRestartTime(long restartTime) {
        this.restartTime = restartTime;
    }

    public boolean isShouldBeRestarted() {
        return shouldBeRestarted;
    }

    public void setShouldBeRestarted(boolean shouldBeRestarted) {
        this.shouldBeRestarted = shouldBeRestarted;
    }

    @Override
    public String toString() {
        return "ActorExecutionException [cause="+getCause()+", restartTime=" + restartTime
                + ", shouldBeRestarted=" + shouldBeRestarted + "]";
    }

}
