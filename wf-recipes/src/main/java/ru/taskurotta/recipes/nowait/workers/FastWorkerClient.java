package ru.taskurotta.recipes.nowait.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

/**
 * Created by void 27.03.13 19:18
 */
@WorkerClient(worker = FastWorker.class)
public interface FastWorkerClient {
	public Promise<Integer> taskB();
	public Promise<Integer> taskC();
	public Promise<Integer> taskD(Promise<Integer> b);
}
