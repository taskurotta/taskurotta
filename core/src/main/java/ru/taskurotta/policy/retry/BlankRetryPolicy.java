package ru.taskurotta.policy.retry;

/**
 * User: stukushin
 * Date: 10.04.13
 * Time: 17:16
 */
public class BlankRetryPolicy extends TimeRetryPolicyBase {
    @Override
    public long nextRetryDelaySeconds(long firstAttempt, long recordedFailure, int numberOfTries) {
        return 0;
    }
}
