package ru.taskurotta.util;

import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.policy.retry.ExponentialRetryPolicy;
import ru.taskurotta.policy.retry.LinearRetryPolicy;
import ru.taskurotta.policy.retry.TimeRetryPolicyBase;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;

/**
 * Created by greg
 */
public final class RetryPolicyConfigUtil {

    public static boolean isRetryable(String failure, RetryPolicyConfigContainer retryPolicyConfig) {
        boolean isRetryable = false;

        for (String exceptionToRetry: retryPolicyConfig.getExceptionsToRetry()) {
            if (exceptionToRetry.equals(failure)) {
                isRetryable = true;
                break;
            }
        }

        if (isRetryable) {
            for (String exceptionNotToRetry: retryPolicyConfig.getExceptionsToExclude()) {
                if (exceptionNotToRetry.equals(failure)) {
                    isRetryable = false;
                    break;
                }
            }
        }

        return isRetryable;
    }

    public static TimeRetryPolicyBase buildTimeRetryPolicy(RetryPolicyConfigContainer retryPolicyConfig) {
        switch (retryPolicyConfig.getType()) {
            case EXPOTENTIAL: {
                ExponentialRetryPolicy retryPolicy = new ExponentialRetryPolicy(retryPolicyConfig.getInitialRetryIntervalSeconds());
                retryPolicy.withMaximumRetryIntervalSeconds(retryPolicyConfig.getMaximumRetryIntervalSeconds());
                retryPolicy.withRetryExpirationIntervalSeconds(retryPolicyConfig.getRetryExpirationIntervalSeconds());
                retryPolicy.withBackoffCoefficient(retryPolicyConfig.getBackoffCoefficient());
                retryPolicy.withMaximumAttempts(retryPolicyConfig.getMaximumAttempts());
                return retryPolicy;
            }
            case LINEAR: {
                LinearRetryPolicy retryPolicy = new LinearRetryPolicy(retryPolicyConfig.getInitialRetryIntervalSeconds());
                retryPolicy.withMaximumRetryIntervalSeconds(retryPolicyConfig.getMaximumRetryIntervalSeconds());
                retryPolicy.withRetryExpirationIntervalSeconds(retryPolicyConfig.getRetryExpirationIntervalSeconds());
                retryPolicy.withMaximumAttempts(retryPolicyConfig.getMaximumAttempts());
                return retryPolicy;
            }
            default:
                return null;
        }
    }
}
