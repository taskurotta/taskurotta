package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.policy.retry.RetryPolicy;

import java.util.concurrent.TimeUnit;

/**
 * User: stukushin, dudin
 * Date: 10.04.13
 * Time: 16:45
 */
public class Inspector {
    public static final String FAILOVER_PROPERTY = "failover";
    private static final Logger logger = LoggerFactory.getLogger(Inspector.class);
    private RetryPolicy retryPolicy; //TaskServer poll policy of inspected actor
    private ActorThreadPool actorThreadPool; //pool of threads for given actor

    private int failoverCheckTime = 60; //time to wait if taskServer unavailable or have no tasks for the actor when retry policy exceeded
    private TimeUnit failoverCheckTimeUnit = TimeUnit.SECONDS;

    protected static  class PolicyCounters {
        long firstAttempt;
        int numberOfTries;

        PolicyCounters(long firstAttempt, int numberOfTries) {
            this.numberOfTries = numberOfTries;
            this.firstAttempt = firstAttempt;
        }
    }

    private ThreadLocal<PolicyCounters> pollCounterThreadLocal = new ThreadLocal<PolicyCounters>();

    public Inspector(RetryPolicy retryPolicy, ActorThreadPool actorThreadPool) {
        this.retryPolicy = retryPolicy;
        this.actorThreadPool = actorThreadPool;
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
                    PolicyCounters policyCounters = getRetryCounter(pollCounterThreadLocal);
                    policyCounters.numberOfTries++;
                    applyRetryPolicy(policyCounters);
                } else {
                    pollCounterThreadLocal.set(null);
                    actorThreadPool.wake();
                }

                return task;
            }

            @Override
            public void release(TaskDecision taskDecision) {
                taskSpreader.release(taskDecision);
            }
        };
    }

    private PolicyCounters getRetryCounter(ThreadLocal<PolicyCounters> source) {
        PolicyCounters result = source.get();
        if (result == null) {
            result = new PolicyCounters(System.currentTimeMillis(), 0);
            source.set(result);
        }
        return result;
    }

    private void applyRetryPolicy(PolicyCounters policyCounters) {
        long nextRetryDelaySeconds = retryPolicy.nextRetryDelaySeconds(policyCounters.firstAttempt, System.currentTimeMillis(), policyCounters.numberOfTries);

        if (nextRetryDelaySeconds < 0) {//maximum attempts exceeded
            logger.trace("Amount to maximum tries (now [{}]) for thread [{}]", policyCounters.numberOfTries, Thread.currentThread().getName());

            if (actorThreadPool.mute()) {//if thread should stop just exit method without unnecessary sleep
                return;
            }

            nextRetryDelaySeconds = failoverCheckTimeUnit.toSeconds(failoverCheckTime);
            logger.info("Communication with TaskServer was idle, waiting for [{}] seconds to continue", nextRetryDelaySeconds);
        }

        try {
            logger.trace("Sleep thread [{}] for [{}] seconds after [{}] tries by retry policy", Thread.currentThread().getName(), nextRetryDelaySeconds, policyCounters.numberOfTries);
            TimeUnit.SECONDS.sleep(nextRetryDelaySeconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setFailover(String timeExpression) {
        if (timeExpression != null) {
            String time = timeExpression.replaceAll("\\D", "").trim();
            if (time.length() > 0) {
                this.failoverCheckTime = Integer.valueOf(time);
            }
            String timeUnit = timeExpression.replaceAll("\\d", "").trim().toUpperCase();
            if (timeUnit.length() > 0) {
                this.failoverCheckTimeUnit = TimeUnit.valueOf(timeUnit);
            }
        }
    }
}
