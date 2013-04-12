package ru.taskurotta.recipes.summator.worker;

/**
 * Created by void 05.04.13 19:13
 */
public class AddWorkerImpl implements AddWorker {

	@Override
	public int add(int a, int b) {
		return a + b;
	}
}
