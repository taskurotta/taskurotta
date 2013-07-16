package ru.taskurotta.recipes.darg.worker;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

@WorkerClient(worker=DArgWorker.class)
public interface DArgWorkerClient {

    public Promise<String> getParam();
    public Promise<Integer> getNumber(Promise<String> param);

}
