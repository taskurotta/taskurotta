package ru.taskurotta.policy.retry;

import ru.taskurotta.policy.PolicyConstants;

/**
 * User: stukushin
 * Date: 08.04.13
 * Time: 13:26
 */
public abstract class TimeRetryPolicyBase extends RetryPolicyBase {
    protected long initialRetryIntervalSeconds;

    protected long maximumRetryIntervalSeconds = PolicyConstants.EXPONENTIAL_RETRY_MAXIMUM_RETRY_INTERVAL_SECONDS;

    protected long retryExpirationIntervalSeconds = PolicyConstants.EXPONENTIAL_RETRY_RETRY_EXPIRATION_SECONDS;

    protected double backoffCoefficient = PolicyConstants.EXPONENTIAL_RETRY_BACKOFF_COEFFICIENT;

    protected int maximumAttempts = PolicyConstants.EXPONENTIAL_RETRY_MAXIMUM_ATTEMPTS;

    public long getInitialRetryIntervalSeconds() {
        return initialRetryIntervalSeconds;
    }

    public long getMaximumRetryIntervalSeconds() {
        return maximumRetryIntervalSeconds;
    }

    /**
     * Set the upper limit of retry interval. No limit by default.
     */
    public void setMaximumRetryIntervalSeconds(long maximumRetryIntervalSeconds) {
        this.maximumRetryIntervalSeconds = maximumRetryIntervalSeconds;
    }

    /**
     * Stop retrying after the specified interval.
     */
    public void setRetryExpirationIntervalSeconds(long retryExpirationIntervalSeconds) {
        this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
    }

    public long getRetryExpirationIntervalSeconds() {
        return retryExpirationIntervalSeconds;
    }

    public double getBackoffCoefficient() {
        return backoffCoefficient;
    }

    /**
     * Coefficient used to calculate the next retry interval. The following
     * formula is used:
     * <code>initialRetryIntervalSeconds * Math.pow(backoffCoefficient, numberOfTries - 2)</code>
     */
    public void setBackoffCoefficient(double backoffCoefficient) {
        this.backoffCoefficient = backoffCoefficient;
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    /**
     * Maximum number of attempts. The first retry is second attempt.
     */
    public void setMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeRetryPolicyBase that = (TimeRetryPolicyBase) o;

        if (Double.compare(that.backoffCoefficient, backoffCoefficient) != 0) return false;
        if (initialRetryIntervalSeconds != that.initialRetryIntervalSeconds) return false;
        if (maximumAttempts != that.maximumAttempts) return false;
        if (maximumRetryIntervalSeconds != that.maximumRetryIntervalSeconds) return false;
        if (retryExpirationIntervalSeconds != that.retryExpirationIntervalSeconds) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (initialRetryIntervalSeconds ^ (initialRetryIntervalSeconds >>> 32));
        result = 31 * result + (int) (maximumRetryIntervalSeconds ^ (maximumRetryIntervalSeconds >>> 32));
        result = 31 * result + (int) (retryExpirationIntervalSeconds ^ (retryExpirationIntervalSeconds >>> 32));
        temp = Double.doubleToLongBits(backoffCoefficient);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + maximumAttempts;
        return result;
    }
}
