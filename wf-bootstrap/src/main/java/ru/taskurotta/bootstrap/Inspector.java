package ru.taskurotta.bootstrap;

import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.policy.retry.RetryPolicy;

import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 10.04.13
 * Time: 16:45
 */
public class Inspector {

    private RetryPolicy retryPolicy;

    protected class PolicyCounters {
        long firstAttempt;
        int numberOfTries;

        PolicyCounters(long firstAttempt, int numberOfTries) {
            this.numberOfTries = numberOfTries;
            this.firstAttempt = firstAttempt;
        }
    }

    private ThreadLocal<PolicyCounters> policyConfigThreadLocal = new ThreadLocal<PolicyCounters>();

    public Inspector(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public RuntimeProcessor decorate(final RuntimeProcessor runtimeProcessor) {
        return runtimeProcessor;
    }

    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {
            @Override
            public Task poll() {
                Task task = taskSpreader.poll();

                if (task == null) {
                    PolicyCounters policyCounters = policyConfigThreadLocal.get();

                    if (policyCounters == null) {
                        policyCounters = new PolicyCounters(System.currentTimeMillis(), 0);
                        policyConfigThreadLocal.set(policyCounters);
                    }

                    policyCounters.numberOfTries++;

                    useRetryPolicy(policyCounters);
                } else {
                    policyConfigThreadLocal.set(null);
                }

                return task;
            }

            @Override
            public void release(TaskDecision taskDecision) {
                taskSpreader.release(taskDecision);
            }
        };
    }

    private void useRetryPolicy(PolicyCounters policyCounters) {
        if (policyCounters.numberOfTries > 2) {
            long nextRetryDelaySeconds = retryPolicy.nextRetryDelaySeconds(policyCounters.firstAttempt, System.currentTimeMillis(), policyCounters.numberOfTries);
            try {
                TimeUnit.SECONDS.sleep(nextRetryDelaySeconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
