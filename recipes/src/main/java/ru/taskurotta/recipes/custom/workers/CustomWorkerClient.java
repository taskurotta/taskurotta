package ru.taskurotta.recipes.custom.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.TaskConfig;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 15.04.13
 * Time: 19:08
 */

@WorkerClient(worker = CustomWorker.class)
public interface CustomWorkerClient {
    public Promise<Integer> sum(int a, int b);

    public Promise<Integer> sum(int a, int b, TaskConfig taskConfig);

    public Promise<Integer> sum(int a, int b, Promise<?> ... waitFor);

    public Promise<Integer> sum(int a, int b, TaskConfig taskConfig, Promise<?> ... waitFor);
}
