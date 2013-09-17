package ru.taskurotta.recipes.wait.worker;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

/**
 * Created by void 13.05.13 19:54
 */
@WorkerClient(worker = WaitWorker.class)
public interface WaitWorkerClient {

    public Promise<Integer> generate();
    public Promise<Integer> prepare();

}
