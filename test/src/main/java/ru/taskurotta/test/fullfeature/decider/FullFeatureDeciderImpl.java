package ru.taskurotta.test.fullfeature.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.NoWait;
import ru.taskurotta.annotation.Wait;
import ru.taskurotta.core.TaskConfig;
import ru.taskurotta.core.Fail;
import ru.taskurotta.core.Promise;
import ru.taskurotta.policy.PolicyConstants;
import ru.taskurotta.policy.retry.RetryPolicyConfig;
import ru.taskurotta.test.fullfeature.worker.FullFeatureWorkerClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by void 20.12.13 17:56
 */
public class FullFeatureDeciderImpl implements FullFeatureDecider {
    protected final static Logger log = LoggerFactory.getLogger(FullFeatureDeciderImpl.class);

    FullFeatureDeciderImpl async;
    FullFeatureWorkerClient worker;

    @Override
    public void start() {
        double[] data = {2,3,4,5};

        TaskConfig options = new TaskConfig().setStartTime(System.currentTimeMillis() + 100l);

        Promise<Double> p01 = worker.sqr(Promise.asPromise(data[0]));
        Promise<Double> p02 = worker.sqr(Promise.asPromise(data[1]), options); //add first
        Promise<Double> res0 = async.step1(p01, p02);

        Promise<Double> p11 = worker.sqr(Promise.asPromise(data[2]));
        Promise<Double> p12 = worker.sqr(Promise.asPromise(data[3]), p01, p02);
        Promise<Double> res1 = async.step1(p11, p12);

        List<Promise<Double>> list = new ArrayList<>(Arrays.asList(res0, res1));
        Promise <Double> result = async.step4(list);
        Promise<Boolean> resultOk = async.isResultOk(data, result);
        async.logResult(resultOk);
    }

    @Asynchronous
    public Promise<Double> step1(Promise<Double> p1, @NoWait Promise<Double> p2) {
        return async.step2(p1.get(), p2);
    }

    @Asynchronous
    public Promise<Double> step2(double p1, Promise<Double> p2) {
        RetryPolicyConfig retryPolicyConfig = new RetryPolicyConfig();
        retryPolicyConfig.setMaximumAttempts(3);
        retryPolicyConfig.setInitialRetryIntervalSeconds(5);
        retryPolicyConfig.setMaximumRetryIntervalSeconds(15);
        retryPolicyConfig.setRetryExpirationIntervalSeconds(PolicyConstants.NONE);
        retryPolicyConfig.setType(RetryPolicyConfig.RetryPolicyType.LINEAR);
        retryPolicyConfig.addExceptionToExclude(java.lang.IllegalArgumentException.class);
        TaskConfig options = new TaskConfig().setStartTime(System.currentTimeMillis() + 100l).setRetryPolicyConfig(retryPolicyConfig);
        Promise<Double> arg = Promise.asPromise(-1 * (p1 + p2.get()));
        Promise<Double> sqrt = worker.sqrt(arg, options);
        return async.step3(arg, sqrt);
    }

    @Asynchronous
    public Promise<Double> step3(Promise<Double> arg, Promise<Double> answer) {
        try {
            Double v = answer.get();
            return Promise.asPromise(v);
        } catch (Fail f) {
            return worker.sqrt(Promise.asPromise(-1 * arg.get()));
        }
    }

    @Asynchronous
    public Promise<Double> step4(@Wait List<Promise<Double>> data) {
        double result = 0;
        for (Promise<Double> val : data) {
            result += val.get();
        }
        return Promise.asPromise(result);
    }

    @Asynchronous
    public Promise<Boolean> isResultOk(double[] d, Promise<Double> result) {
        double val = Math.sqrt(d[0] * d[0] + d[1] * d[1]) + Math.sqrt(d[2] * d[2] + d[3] * d[3]);
        boolean resultOk = val - result.get() < 1e-6;
        return Promise.asPromise(resultOk);
    }

    @Asynchronous
    public void logResult(Promise<Boolean> result) {
        log.info("is process correct: {}", result.get());
    }


    public void setAsync(FullFeatureDeciderImpl async) {
        this.async = async;
    }

    public void setWorker(FullFeatureWorkerClient worker) {
        this.worker = worker;
    }
}
