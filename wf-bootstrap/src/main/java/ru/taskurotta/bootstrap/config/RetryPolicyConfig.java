package ru.taskurotta.bootstrap.config;

import ru.taskurotta.policy.retry.RetryPolicy;

/**
 * User: stukushin
 * Date: 08.04.13
 * Time: 12:29
 */
public interface RetryPolicyConfig {
    public RetryPolicy getPolicy();
}
