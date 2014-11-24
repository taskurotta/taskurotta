package ru.taskurotta.transport.model;

import ru.taskurotta.core.RetryPolicyConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg
 */
public class RetryPolicyConfigContainer implements Serializable {
    private RetryPolicyConfig.RetryPolicyType type;
    private long initialRetryIntervalSeconds;
    private long maximumRetryIntervalSeconds;
    private long retryExpirationIntervalSeconds;
    private double backoffCoefficient;
    private int maximumAttempts;
    private List<String> exceptionsToRetry = new ArrayList<String>();
    private List<String> exceptionsToExclude = new ArrayList<String>();

    public RetryPolicyConfigContainer(){

    }

    public RetryPolicyConfigContainer(
            RetryPolicyConfig.RetryPolicyType type,
            long initialRetryIntervalSeconds,
            long maximumRetryIntervalSeconds,
            long retryExpirationIntervalSeconds,
            double backoffCoefficient,
            int maximumAttempts,
            List<String> exceptionsToRetry,
            List<String> exceptionsToExclude) {
        this.type = type;
        this.initialRetryIntervalSeconds = initialRetryIntervalSeconds;
        this.maximumRetryIntervalSeconds = maximumRetryIntervalSeconds;
        this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
        this.backoffCoefficient = backoffCoefficient;
        this.maximumAttempts = maximumAttempts;
        this.exceptionsToRetry = exceptionsToRetry;
        this.exceptionsToExclude = exceptionsToExclude;
    }

    public RetryPolicyConfig.RetryPolicyType getType() {
        return type;
    }

    public void setType(RetryPolicyConfig.RetryPolicyType type) {
        this.type = type;
    }

    public long getInitialRetryIntervalSeconds() {
        return initialRetryIntervalSeconds;
    }

    public void setInitialRetryIntervalSeconds(long initialRetryIntervalSeconds) {
        this.initialRetryIntervalSeconds = initialRetryIntervalSeconds;
    }

    public long getMaximumRetryIntervalSeconds() {
        return maximumRetryIntervalSeconds;
    }

    public void setMaximumRetryIntervalSeconds(long maximumRetryIntervalSeconds) {
        this.maximumRetryIntervalSeconds = maximumRetryIntervalSeconds;
    }

    public long getRetryExpirationIntervalSeconds() {
        return retryExpirationIntervalSeconds;
    }

    public void setRetryExpirationIntervalSeconds(long retryExpirationIntervalSeconds) {
        this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
    }

    public double getBackoffCoefficient() {
        return backoffCoefficient;
    }

    public void setBackoffCoefficient(double backoffCoefficient) {
        this.backoffCoefficient = backoffCoefficient;
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public void setMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts;
    }

    public List<String> getExceptionsToRetry() {
        return exceptionsToRetry;
    }

    public void setExceptionsToRetry(List<String> exceptionsToRetry) {
        this.exceptionsToRetry = exceptionsToRetry;
    }

    public List<String> getExceptionsToExclude() {
        return exceptionsToExclude;
    }

    public void setExceptionsToExclude(List<String> exceptionsToExclude) {
        this.exceptionsToExclude = exceptionsToExclude;
    }

    public void addExceptionToRetryException(Class<? extends Throwable> clazz) {
        exceptionsToRetry.add(clazz.getName());
    }

    public void addExceptionToExclude(Class<? extends Throwable> clazz) {
        exceptionsToExclude.add(clazz.getName());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RetryPolicyConfigContainer that = (RetryPolicyConfigContainer) o;

        if (Double.compare(that.backoffCoefficient, backoffCoefficient) != 0) return false;
        if (initialRetryIntervalSeconds != that.initialRetryIntervalSeconds) return false;
        if (maximumAttempts != that.maximumAttempts) return false;
        if (maximumRetryIntervalSeconds != that.maximumRetryIntervalSeconds) return false;
        if (retryExpirationIntervalSeconds != that.retryExpirationIntervalSeconds) return false;
        if (exceptionsToExclude != null ? !exceptionsToExclude.equals(that.exceptionsToExclude) : that.exceptionsToExclude != null)
            return false;
        if (exceptionsToRetry != null ? !exceptionsToRetry.equals(that.exceptionsToRetry) : that.exceptionsToRetry != null)
            return false;
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
        result = 31 * result + (exceptionsToRetry != null ? exceptionsToRetry.hashCode() : 0);
        result = 31 * result + (exceptionsToExclude != null ? exceptionsToExclude.hashCode() : 0);
        return result;
    }
}
