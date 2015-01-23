package ru.taskurotta.recipes.retrypolicy.decider;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.core.TaskConfig;
import ru.taskurotta.recipes.retrypolicy.workers.SumWorkerClient;

/**
 * User: stukushin
 * Date: 11.04.13
 * Time: 20:06
 */
public class SumDeciderImpl implements SumDecider {
    private SumWorkerClient sumWorker;
    private SumDeciderImpl asynchronous;

    @Override
    public void calculate(int a, int b) {
        RetryPolicyConfig retryPolicyConfig = new RetryPolicyConfig(RetryPolicyConfig.RetryPolicyType.LINEAR, 1, 10, -1, 2.0, 5);
        retryPolicyConfig.addExceptionToRetry(RuntimeException.class);

        TaskConfig taskConfig = new TaskConfig();
        taskConfig.setRetryPolicyConfig(retryPolicyConfig);

        Promise<Integer> sumWithClientSideRetryPolicy = sumWorker.sum(a, b);
        Promise<Integer> sumWithServerSideRetryPolicy = sumWorker.sum(a, b, taskConfig);

        asynchronous.show(a, b, sumWithClientSideRetryPolicy, sumWithServerSideRetryPolicy);
    }

    @Asynchronous
    public void show(int a, int b, Promise<Integer> sumWithClientSideRetryPolicy,  Promise<Integer> sumWithServerSideRetryPolicy) {
        System.out.println("With client side retry policy: " + a + " + " + b + " = " + sumWithClientSideRetryPolicy.get());
        System.out.println("With server side retry policy: " + a + " + " + b + " = " + sumWithServerSideRetryPolicy.get());
        System.exit(0);
    }

    public void setSumWorker(SumWorkerClient sumWorker) {
        this.sumWorker = sumWorker;
    }

    public void setAsynchronous(SumDeciderImpl asynchronous) {
        this.asynchronous = asynchronous;
    }
}
