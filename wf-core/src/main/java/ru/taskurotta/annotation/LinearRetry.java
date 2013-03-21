package ru.taskurotta.annotation;

import ru.taskurotta.policy.PolicyConstants;

/**
 * User: stukushin
 * Date: 21.02.13
 * Time: 13:18
 */
public @interface LinearRetry {
    /**
     * Interval to wait after the initial failure, before triggering a retry.
     * <p>
     * This value should not be greater than values specified for
     * maximumRetryPeriod or retryExpirationPeriod.
     */
    long initialRetryIntervalSeconds();

    /**
     * Maximum interval to wait between retry attempts.
     * <p>
     * This value should not be less than value specified for
     * initialRetryPeriod. Default value is unlimited.
     */
    long maximumRetryIntervalSeconds() default PolicyConstants.EXPONENTIAL_RETRY_MAXIMUM_RETRY_INTERVAL_SECONDS;

    /**
     * Total duration across all attempts before giving up and attempting
     * no further retries.
     * <p>
     * This duration is measured relative to the initial attempt's starting time.
     * and
     * <p>
     * This value should not be less than value specified for
     * initialRetryPeriod. Default value is unlimited.
     */
    long retryExpirationSeconds() default PolicyConstants.EXPONENTIAL_RETRY_RETRY_EXPIRATION_SECONDS;

    /**
     * Number of maximum retry attempts (including the initial attempt).
     * Default value is no limit.
     */
    int maximumAttempts() default PolicyConstants.EXPONENTIAL_RETRY_MAXIMUM_ATTEMPTS;

    /**
     * Default is {@link Throwable} which means that all exceptions are retried.
     */
    Class<? extends Throwable>[] exceptionsToRetry() default { Throwable.class };

    /**
     * What exceptions that match exceptionsToRetry list should be not retried.
     * Default is empty list.
     */
    Class<? extends Throwable>[] excludeExceptions() default {};
}
