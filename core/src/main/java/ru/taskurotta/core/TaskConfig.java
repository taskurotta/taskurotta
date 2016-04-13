package ru.taskurotta.core;


/**
 * Date: 15.04.13 16:45
 */
public class TaskConfig {

    private String customId;
    private long startTime = -1;
    private String taskList; // name of task queue/list
    private RetryPolicyConfig retryPolicyConfig;
    private long timeout = -1;

    public String getCustomId() {
        return customId;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getTaskList() {
        return taskList;
    }

    public RetryPolicyConfig getRetryPolicyConfig() {
        return retryPolicyConfig;
    }

    public TaskConfig setCustomId(String customId) {
        this.customId = customId;
        return this;
    }

    public TaskConfig setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    public TaskConfig setTaskList(String taskList) {
        this.taskList = taskList;
        return this;
    }

    public TaskConfig setRetryPolicyConfig(RetryPolicyConfig retryPolicyConfig) {
        this.retryPolicyConfig = retryPolicyConfig;
        return this;
    }

    public long getTimeout() {
        return timeout;
    }

    public TaskConfig setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public TaskConfig withRetryPolicyConfig(RetryPolicyConfig.RetryPolicyType type,
                                            long initialRetryIntervalSeconds,
                                            long maximumRetryIntervalSeconds,
                                            long retryExpirationIntervalSeconds,
                                            double backoffCoefficient,
                                            int maximumAttempts) {
        RetryPolicyConfig rps = new RetryPolicyConfig(type, initialRetryIntervalSeconds, maximumRetryIntervalSeconds, retryExpirationIntervalSeconds, backoffCoefficient, maximumAttempts);
        setRetryPolicyConfig(rps);
        return this;
    }

    public TaskConfig withRetryPolicyConfig(RetryPolicyConfig.RetryPolicyType type,
                                            long initialRetryIntervalSeconds,
                                            long maximumRetryIntervalSeconds,
                                            long retryExpirationIntervalSeconds,
                                            double backoffCoefficient,
                                            int maximumAttempts,
                                            Class<? extends Throwable>[] exceptionToRetry,
                                            Class<? extends Throwable>[] exceptionToExclude) {
        RetryPolicyConfig rps = new RetryPolicyConfig(type, initialRetryIntervalSeconds, maximumRetryIntervalSeconds, retryExpirationIntervalSeconds, backoffCoefficient, maximumAttempts);
        for (Class<? extends Throwable> clazzRetry : exceptionToRetry) {
            rps.addExceptionToRetry(clazzRetry);
        }
        for (Class<? extends Throwable> clazzExclude : exceptionToExclude) {
            rps.addExceptionToExclude(clazzExclude);
        }
        setRetryPolicyConfig(rps);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskConfig that = (TaskConfig) o;

        if (startTime != that.startTime) return false;
        if (timeout != that.timeout) return false;
        if (customId != null ? !customId.equals(that.customId) : that.customId != null) return false;
        if (taskList != null ? !taskList.equals(that.taskList) : that.taskList != null) return false;
        return retryPolicyConfig != null ? retryPolicyConfig.equals(that.retryPolicyConfig) : that.retryPolicyConfig == null;

    }

    @Override
    public int hashCode() {
        int result = customId != null ? customId.hashCode() : 0;
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (taskList != null ? taskList.hashCode() : 0);
        result = 31 * result + (retryPolicyConfig != null ? retryPolicyConfig.hashCode() : 0);
        result = 31 * result + (int) (timeout ^ (timeout >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "TaskConfig{" +
                "customId='" + customId + '\'' +
                ", startTime=" + startTime +
                ", taskList='" + taskList + '\'' +
                ", retryPolicyConfig=" + retryPolicyConfig +
                ", timeout=" + timeout +
                '}';
    }
}
