package ru.taskurotta.core;

import ru.taskurotta.policy.retry.BlankRetryPolicy;
import ru.taskurotta.policy.retry.ExponentialRetryPolicy;
import ru.taskurotta.policy.retry.LinearRetryPolicy;
import ru.taskurotta.policy.retry.TimeRetryPolicyBase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg
 */
public class RetryPolicyConfig implements Serializable {

    private RetryPolicyType type;
    private long initialRetryIntervalSeconds;
    private long maximumRetryIntervalSeconds;
    private long retryExpirationIntervalSeconds;
    private double backoffCoefficient;
    private int maximumAttempts;

    private List<String> exceptionsToRetry = new ArrayList<String>();
    private List<String> exceptionsToExclude = new ArrayList<String>();

    public RetryPolicyConfig(){

    }

    public RetryPolicyConfig(
            RetryPolicyType type,
            long initialRetryIntervalSeconds,
            long maximumRetryIntervalSeconds,
            long retryExpirationIntervalSeconds,
            double backoffCoefficient,
            int maximumAttempts
    ) {
        this.type = type;
        this.initialRetryIntervalSeconds = initialRetryIntervalSeconds;
        this.maximumRetryIntervalSeconds = maximumRetryIntervalSeconds;
        this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
        this.backoffCoefficient = backoffCoefficient;
        this.maximumAttempts = maximumAttempts;
    }

    public RetryPolicyType getType() {
        return type;
    }

    public RetryPolicyConfig setType(RetryPolicyType type) {
        this.type = type;
        return this;
    }

    public long getInitialRetryIntervalSeconds() {
        return initialRetryIntervalSeconds;
    }

    public RetryPolicyConfig setInitialRetryIntervalSeconds(long initialRetryIntervalSeconds) {
        this.initialRetryIntervalSeconds = initialRetryIntervalSeconds;
        return this;
    }

    public long getMaximumRetryIntervalSeconds() {
        return maximumRetryIntervalSeconds;
    }

    public RetryPolicyConfig setMaximumRetryIntervalSeconds(long maximumRetryIntervalSeconds) {
        this.maximumRetryIntervalSeconds = maximumRetryIntervalSeconds;
        return this;
    }

    public long getRetryExpirationIntervalSeconds() {
        return retryExpirationIntervalSeconds;
    }

    public RetryPolicyConfig setRetryExpirationIntervalSeconds(long retryExpirationIntervalSeconds) {
        this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
        return this;
    }

    public double getBackoffCoefficient() {
        return backoffCoefficient;
    }

    public RetryPolicyConfig setBackoffCoefficient(double backoffCoefficient) {
        this.backoffCoefficient = backoffCoefficient;
        return this;
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public RetryPolicyConfig setMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts;
        return this;
    }

    public List<String> getExceptionsToRetry() {
        return exceptionsToRetry;
    }

    public List<String> getExceptionsToExclude() {
        return exceptionsToExclude;
    }



    public void addExceptionToRetryException(Class<? extends Throwable> clazz) {
        exceptionsToRetry.add(clazz.getName());
    }

    public void addExceptionToExclude(Class<? extends Throwable> clazz) {
        exceptionsToExclude.add(clazz.getName());
    }



    public enum RetryPolicyType {
        LINEAR(0), EXPOTENTIAL(1);

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
                    return RetryPolicyType.LINEAR;
                case 1:
                    return RetryPolicyType.EXPOTENTIAL;
            }
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RetryPolicyConfig that = (RetryPolicyConfig) o;

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
        return "RetryPolicyConfig{" +
                "type=" + type +
                ", initialRetryIntervalSeconds=" + initialRetryIntervalSeconds +
                ", maximumRetryIntervalSeconds=" + maximumRetryIntervalSeconds +
                ", retryExpirationIntervalSeconds=" + retryExpirationIntervalSeconds +
                ", backoffCoefficient=" + backoffCoefficient +
                ", maximumAttempts=" + maximumAttempts +
                ", exceptionsToRetry=" + exceptionsToRetry +
                ", exceptionsToExclude=" + exceptionsToExclude +
                '}';
    }
}
