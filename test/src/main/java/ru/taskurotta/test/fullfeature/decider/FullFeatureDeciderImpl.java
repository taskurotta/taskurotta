package ru.taskurotta.test.fullfeature.decider;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.NoWait;
import ru.taskurotta.annotation.Wait;
import ru.taskurotta.core.Fail;
import ru.taskurotta.core.Promise;
import ru.taskurotta.internal.core.ActorSchedulingOptionsImpl;
import ru.taskurotta.test.fullfeature.worker.FullFeatureWorkerClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by void 20.12.13 17:56
 */
public class FullFeatureDeciderImpl implements FullFeatureDecider {

    FullFeatureDeciderImpl async;
    FullFeatureWorkerClient worker;

    @Override
    public void start() {
        double[] data = {2,3,4,5};

        ActorSchedulingOptionsImpl options = new ActorSchedulingOptionsImpl();
        options.setStartTime(System.currentTimeMillis() + 100);

        Promise<Double> p0 = worker.sqr(Promise.asPromise(data[0]));
        Promise<Double> p1 = worker.sqr(Promise.asPromise(data[1]), options);
        Promise<Double> res0 = async.step1(p0, p1);

        Promise<Double> p2 = worker.sqr(Promise.asPromise(data[2]));
        Promise<Double> p3 = worker.sqr(Promise.asPromise(data[3]));
        Promise<Double> res1 = async.step1(p2, p3);

        List<Promise<Double>> list = new ArrayList<>(Arrays.asList(res0, res1));
        Promise <Double> result = async.step4(list);
        async.isResultOk(data, result);
    }

    @Asynchronous
    public Promise<Double> step1(Promise<Double> p1, @NoWait Promise<Double> p2) {
        return step2(p1.get(), p2);
    }

    @Asynchronous
    public Promise<Double> step2(double p1, Promise<Double> p2) {
        Promise<Double> arg = Promise.asPromise(-1 * (p1 + p2.get()));
        Promise<Double> sqrt = worker.sqrt(arg);
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


    public void setAsync(FullFeatureDeciderImpl async) {
        this.async = async;
    }

    public void setWorker(FullFeatureWorkerClient worker) {
        this.worker = worker;
    }
}
