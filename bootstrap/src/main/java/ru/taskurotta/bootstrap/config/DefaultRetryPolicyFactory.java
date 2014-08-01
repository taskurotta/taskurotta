package ru.taskurotta.bootstrap.config;

import ru.taskurotta.policy.retry.RetryPolicy;
import ru.taskurotta.policy.retry.TimeRetryPolicyBase;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * User: stukushin
 * Date: 08.04.13
 * Time: 12:42
 */
public class DefaultRetryPolicyFactory implements RetryPolicyFactory {

    private Properties properties;
    private String className;

    @Override
    public RetryPolicy getRetryPolicy() {
        TimeRetryPolicyBase retryPolicy;

        long initialRetryIntervalSeconds = properties.containsKey("initialRetryIntervalSeconds") ?
                Long.parseLong(String.valueOf(properties.get("initialRetryIntervalSeconds"))) : 10;

        try {
            retryPolicy = (TimeRetryPolicyBase) Class.forName(className).getConstructor(long.class).newInstance(initialRetryIntervalSeconds);

            if (properties.containsKey("maximumRetryIntervalSeconds")) {
                retryPolicy.setMaximumRetryIntervalSeconds(Long.parseLong(String.valueOf(properties.get("maximumRetryIntervalSeconds"))));
            }

            if (properties.containsKey("retryExpirationIntervalSeconds")) {
                retryPolicy.setRetryExpirationIntervalSeconds(Long.parseLong(String.valueOf(properties.get("retryExpirationIntervalSeconds"))));
            }

            if (properties.containsKey("backoffCoefficient")) {
                retryPolicy.setBackoffCoefficient(Double.parseDouble(String.valueOf(properties.get("backoffCoefficient"))));
            }

            if (properties.containsKey("maximumAttempts")) {
                retryPolicy.setMaximumAttempts(Integer.parseInt(String.valueOf(properties.get("maximumAttempts"))));
            }
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return retryPolicy;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setClass(String className) {
        this.className = className;
    }
}
