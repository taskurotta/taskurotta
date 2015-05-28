package ru.taskurotta.test.fat.response.impl;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.test.fat.response.FatWorker;
import ru.taskurotta.test.fat.response.Response;

/**
 * Created on 28.05.2015.
 */
@WorkerClient(worker = FatWorker.class)
public interface FatWorkerClient {

    Promise<String> createResponse(int size);

}
