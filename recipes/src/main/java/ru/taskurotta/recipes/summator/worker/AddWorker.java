package ru.taskurotta.recipes.summator.worker;

import ru.taskurotta.annotation.Worker;

/**
 * Created by void 05.04.13 19:09
 */
@Worker
public interface AddWorker {

	public int add(int a, int b);
}
