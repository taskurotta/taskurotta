package ru.taskurotta.bootstrap;

import ru.taskurotta.annotation.WorkerClient;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 18:32
 */
@WorkerClient(worker = TestWorker.class)
public interface TestWorkerClient {
    public int sum(int a, int b);
}
