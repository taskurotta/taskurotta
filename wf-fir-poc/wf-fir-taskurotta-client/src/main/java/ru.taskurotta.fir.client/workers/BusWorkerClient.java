package ru.taskurotta.fir.client.workers;

import ru.taskurotta.annotation.WorkerClient;

/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 11:04
 */

@WorkerClient(worker = BusWorker.class)
public interface BusWorkerClient {
    public void sendPackage(String uuid, int result);
}
