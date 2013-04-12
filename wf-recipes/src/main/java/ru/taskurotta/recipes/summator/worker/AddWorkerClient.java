package ru.taskurotta.recipes.summator.worker;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

/**
 * Created by void 05.04.13 19:11
 */
@WorkerClient(worker = AddWorker.class)
public interface AddWorkerClient {
	public Promise<Integer> add(Promise<Integer> a, Promise<Integer> b);
}
