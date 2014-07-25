package ru.taskurotta.policy.retry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg
 */
public class RetryPolicySettings implements Serializable {

    private RetryPolicyType type;
    private long initialRetryIntervalSeconds;
    private long maximumRetryIntervalSeconds;
    private long retryExpirationIntervalSeconds;
    private double backoffCoefficient;
    private int maximumAttempts;

    private List<String> exceptionsToRetry = new ArrayList<String>();
    private List<String> exceptionsToExclude = new ArrayList<String>();

    public RetryPolicyType getType() {
        return type;
    }

    public void setType(RetryPolicyType type) {
        this.type = type;
    }

    public long getInitialRetryIntervalSeconds() {
        return initialRetryIntervalSeconds;
    }

    public RetryPolicySettings setInitialRetryIntervalSeconds(long initialRetryIntervalSeconds) {
        this.initialRetryIntervalSeconds = initialRetryIntervalSeconds;
        return this;
    }

    public long getMaximumRetryIntervalSeconds() {
        return maximumRetryIntervalSeconds;
    }

    public RetryPolicySettings setMaximumRetryIntervalSeconds(long maximumRetryIntervalSeconds) {
        this.maximumRetryIntervalSeconds = maximumRetryIntervalSeconds;
        return this;
    }

    public long getRetryExpirationIntervalSeconds() {
        return retryExpirationIntervalSeconds;
    }

    public RetryPolicySettings setRetryExpirationIntervalSeconds(long retryExpirationIntervalSeconds) {
        this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
        return this;
    }

    public double getBackoffCoefficient() {
        return backoffCoefficient;
    }

    public RetryPolicySettings setBackoffCoefficient(double backoffCoefficient) {
        this.backoffCoefficient = backoffCoefficient;
        return this;
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public RetryPolicySettings setMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts;
        return this;
    }

    public List<String> getExceptionsToRetry() {
        return exceptionsToRetry;
    }

    public List<String> getExceptionsToExclude() {
        return exceptionsToExclude;
    }

    public TimeRetryPolicyBase buildTimeRetryPolicy() {
        switch (type) {
            case EXPOTENTIAL: {
                ExponentialRetryPolicy retryPolicy = new ExponentialRetryPolicy(initialRetryIntervalSeconds);
                retryPolicy.withMaximumRetryIntervalSeconds(getMaximumRetryIntervalSeconds());
                retryPolicy.withRetryExpirationIntervalSeconds(getRetryExpirationIntervalSeconds());
                retryPolicy.withBackoffCoefficient(getBackoffCoefficient());
                retryPolicy.withMaximumAttempts(getMaximumAttempts());
                return retryPolicy;
            }
            case LINEAR: {
                LinearRetryPolicy retryPolicy = new LinearRetryPolicy(initialRetryIntervalSeconds);
                retryPolicy.withMaximumRetryIntervalSeconds(getMaximumRetryIntervalSeconds());
                retryPolicy.withRetryExpirationIntervalSeconds(getRetryExpirationIntervalSeconds());
                retryPolicy.withMaximumAttempts(getMaximumAttempts());
                return retryPolicy;
            }
            default:
                return new BlankRetryPolicy();
        }
    }

    public void addExceptionToRetryException(Class<? extends Throwable> clazz) {
        exceptionsToRetry.add(clazz.getName());
    }

    public void addExceptionToExclude(Class<? extends Throwable> clazz) {
        exceptionsToExclude.add(clazz.getName());
    }

    public boolean isExceptionToRetry(String exceptionClass) {
        return exceptionsToRetry.contains(exceptionClass);
    }

    public boolean isExceptionToExclude(String exceptionClass) {
        return exceptionsToExclude.contains(exceptionClass);
    }

    public enum RetryPolicyType {
        BLANK(0), LINEAR(1), EXPOTENTIAL(2);

        private int value;

        RetryPolicyType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static RetryPolicyType build(int value) {
            switch (value) {
                case 0:
                    return RetryPolicyType.BLANK;
                case 1:
                    return RetryPolicyType.LINEAR;
                case 2:
                    return RetryPolicyType.EXPOTENTIAL;
            }
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RetryPolicySettings that = (RetryPolicySettings) o;

        if (Double.compare(that.backoffCoefficient, backoffCoefficient) != 0) return false;
        if (initialRetryIntervalSeconds != that.initialRetryIntervalSeconds) return false;
        if (maximumAttempts != that.maximumAttempts) return false;
        if (maximumRetryIntervalSeconds != that.maximumRetryIntervalSeconds) return false;
        if (retryExpirationIntervalSeconds != that.retryExpirationIntervalSeconds) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = type != null ? type.hashCode() : 0;
        result = 31 * result + (int) (initialRetryIntervalSeconds ^ (initialRetryIntervalSeconds >>> 32));
        result = 31 * result + (int) (maximumRetryIntervalSeconds ^ (maximumRetryIntervalSeconds >>> 32));
        result = 31 * result + (int) (retryExpirationIntervalSeconds ^ (retryExpirationIntervalSeconds >>> 32));
        temp = Double.doubleToLongBits(backoffCoefficient);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + maximumAttempts;
        return result;
    }

    @Override
    public String toString() {
        return "RetryPolicySettings{" +
                "type=" + type +
                ", initialRetryIntervalSeconds=" + initialRetryIntervalSeconds +
                ", maximumRetryIntervalSeconds=" + maximumRetryIntervalSeconds +
                ", retryExpirationIntervalSeconds=" + retryExpirationIntervalSeconds +
                ", backoffCoefficient=" + backoffCoefficient +
                ", maximumAttempts=" + maximumAttempts +
                '}';
    }
}
