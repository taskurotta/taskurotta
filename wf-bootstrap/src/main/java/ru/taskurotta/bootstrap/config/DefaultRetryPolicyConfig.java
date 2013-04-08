package ru.taskurotta.bootstrap.config;

import ru.taskurotta.policy.retry.RetryPolicy;

import java.util.Properties;

/**
 * User: stukushin
 * Date: 08.04.13
 * Time: 12:42
 */
public class DefaultRetryPolicyConfig implements RetryPolicyConfig {

    private Properties properties;
    private String className;

    @Override
    public RetryPolicy getPolicy() {
        RetryPolicy retryPolicy = null;

        long initialRetryIntervalSeconds = properties.containsKey("initialRetryIntervalSeconds") ?
                Long.parseLong(String.valueOf(properties.get("initialRetryIntervalSeconds"))) : 10;

        RetryPolicy

        return retryPolicy;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
