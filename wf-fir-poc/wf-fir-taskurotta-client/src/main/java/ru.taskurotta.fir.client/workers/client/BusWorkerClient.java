package ru.taskurotta.fir.client.workers.client;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.fir.client.workers.BusWorker;

/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 11:04
 */

@WorkerClient(worker = BusWorker.class)
public interface BusWorkerClient {
    public void sendPackage(String uuid, int result);
}
