package ru.taskurotta.recipes.custom.workers;

/**
 * User: stukushin
 * Date: 15.04.13
 * Time: 19:09
 */
public class CustomWorkerImpl implements CustomWorker {
    @Override
    public int sum(int a, int b) {
        return a + b;
    }
}
