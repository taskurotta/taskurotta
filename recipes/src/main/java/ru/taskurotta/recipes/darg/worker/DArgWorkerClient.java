package ru.taskurotta.recipes.darg.worker;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

@WorkerClient(worker=DArgWorker.class)
public interface DArgWorkerClient {

    public Promise<String> getParam();

    public Promise<String> processParams(String param1, Promise<String> param2, Promise<String> param3, Promise<String> param4);

    public Promise<Integer> getNumber(Promise<String> param);

}
