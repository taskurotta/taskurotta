package ru.taskurotta.bootstrap;

import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.policy.retry.RetryPolicy;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 10.04.13
 * Time: 13:16
 */
public class PolicyArbiter {

    private int poolTries;
    private Date poolFirstAttempt;

    private int executeTries;
    private Date executeFirstAttempt;

    private RetryPolicy retryPolicy;

    public PolicyArbiter(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public boolean continueAfterPoll(Task task) {
        if (task == null) {
            poolTries++;

            if (poolFirstAttempt == null) {
                poolFirstAttempt = new Date();
            }

            useRetryPolicy(poolFirstAttempt, poolTries);

            return false;
        } else {
            poolTries = 0;
            poolFirstAttempt = null;

            return true;
        }
    }

    public boolean continueAfterExecute(TaskDecision taskDecision) {
        if (taskDecision.isError()) {
            executeTries++;

            if (executeFirstAttempt == null) {
                executeFirstAttempt = new Date();
            }

            useRetryPolicy(executeFirstAttempt, executeTries);

            return false;
        } else {
            executeTries = 0;
            executeFirstAttempt = null;

            return true;
        }
    }

    private void useRetryPolicy(Date firstAttempt, int numberOfTries) {
        if (numberOfTries > 2) {
            long nextRetryDelaySeconds = retryPolicy.nextRetryDelaySeconds(firstAttempt, new Date(), numberOfTries);
            try {
                TimeUnit.SECONDS.sleep(nextRetryDelaySeconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
