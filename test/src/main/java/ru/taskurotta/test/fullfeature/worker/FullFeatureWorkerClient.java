package ru.taskurotta.test.fullfeature.worker;

import ru.taskurotta.annotation.AcceptFail;
import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.ActorSchedulingOptions;
import ru.taskurotta.core.Promise;

/**
 * Created by void 20.12.13 18:03
 */
@WorkerClient(worker = FullFeatureWorker.class)
public interface FullFeatureWorkerClient {

    Promise<Double> sqr(Promise<Double> a);
    Promise<Double> sqr(Promise<Double> a, ActorSchedulingOptions options);

    @AcceptFail(type = IllegalArgumentException.class)
    Promise<Double> sqrt(Promise<Double> a);
}
