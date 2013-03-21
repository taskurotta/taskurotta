package ru.taskurotta.policy.retry;

import ru.taskurotta.policy.PolicyConstants;

import java.util.Collection;
import java.util.Date;

/**
 * User: stukushin
 * Date: 21.02.13
 * Time: 13:21
 */
public class LinearRetryPolicy extends RetryPolicyBase {
    private final long initialRetryIntervalSeconds;

    private long maximumRetryIntervalSeconds = PolicyConstants.EXPONENTIAL_RETRY_MAXIMUM_RETRY_INTERVAL_SECONDS;

    private long retryExpirationIntervalSeconds = PolicyConstants.EXPONENTIAL_RETRY_RETRY_EXPIRATION_SECONDS;

    private int maximumAttempts = PolicyConstants.EXPONENTIAL_RETRY_MAXIMUM_ATTEMPTS;

    public LinearRetryPolicy(long initialRetryIntervalSeconds) {
        this.initialRetryIntervalSeconds = initialRetryIntervalSeconds;
    }

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

    public LinearRetryPolicy withMaximumRetryIntervalSeconds(long maximumRetryIntervalSeconds) {
        this.maximumRetryIntervalSeconds = maximumRetryIntervalSeconds;
        return this;
    }

    public long getRetryExpirationIntervalSeconds() {
        return retryExpirationIntervalSeconds;
    }

    /**
     * Stop retrying after the specified interval.
     */
    public void setRetryExpirationIntervalSeconds(long retryExpirationIntervalSeconds) {
        this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
    }

    public LinearRetryPolicy withRetryExpirationIntervalSeconds(long retryExpirationIntervalSeconds) {
        this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
        return this;
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

    public LinearRetryPolicy withMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts;
        return this;
    }

    /**
     * The exception types that cause operation being retried. Subclasses of the
     * specified types are also included. Default is Throwable.class which means
     * retry any exceptions.
     */
    @Override
    public LinearRetryPolicy withExceptionsToRetry(Collection<Class<? extends Throwable>> exceptionsToRetry) {
        super.withExceptionsToRetry(exceptionsToRetry);
        return this;
    }

    /**
     * The exception types that should not be retried. Subclasses of the
     * specified types are also not retried. Default is empty list.
     */
    @Override
    public LinearRetryPolicy withExceptionsToExclude(Collection<Class<? extends Throwable>> exceptionsToRetry) {
        super.withExceptionsToExclude(exceptionsToRetry);
        return this;
    }

    @Override
    public long nextRetryDelaySeconds(Date firstAttempt, Date recordedFailure, int numberOfTries) {

        if (numberOfTries < 2) {
            throw new IllegalArgumentException("attempt is less then 2: " + numberOfTries);
        }

        if (maximumAttempts > PolicyConstants.NONE && numberOfTries > maximumAttempts) {
            return PolicyConstants.NONE;
        }

        long result = initialRetryIntervalSeconds;
        result = maximumRetryIntervalSeconds > PolicyConstants.NONE ? Math.min(result, maximumRetryIntervalSeconds) : result;
        int secondsSinceFirstAttempt = (int) ((recordedFailure.getTime() - firstAttempt.getTime()) / 1000);
        if (retryExpirationIntervalSeconds > PolicyConstants.NONE && secondsSinceFirstAttempt + result >= retryExpirationIntervalSeconds) {
            return PolicyConstants.NONE;
        }

        return result;
    }

    /**
     * Performs the following three validation checks for LinearRetryPolicy
     * Policy: 1) initialRetryIntervalSeconds is not greater than
     * maximumRetryIntervalSeconds 2) initialRetryIntervalSeconds is not greater
     * than retryExpirationIntervalSeconds
     */
    public void validate() throws IllegalStateException {
        if (maximumRetryIntervalSeconds > PolicyConstants.NONE && initialRetryIntervalSeconds > maximumRetryIntervalSeconds) {
            throw new IllegalStateException(
                    "LinearRetryPolicy requires maximumRetryIntervalSeconds to have a value larger than initialRetryIntervalSeconds.");
        }

        if (retryExpirationIntervalSeconds > PolicyConstants.NONE && initialRetryIntervalSeconds > retryExpirationIntervalSeconds) {
            throw new IllegalStateException(
                    "LinearRetryPolicy requires retryExpirationIntervalSeconds to have a value larger than initialRetryIntervalSeconds.");
        }
    }
}
