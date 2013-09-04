package ru.taskurotta.recipes.parallel.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 18.03.13
 * Time: 14:46
 */
@WorkerClient(worker = PiWorker.class)
public interface PiWorkerClient {
    public Promise<Double> calculate(long start, long elements);
}
