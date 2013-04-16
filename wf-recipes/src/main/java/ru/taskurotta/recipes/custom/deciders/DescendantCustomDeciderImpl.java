package ru.taskurotta.recipes.custom.deciders;

import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.custom.workers.CustomWorkerClient;

/**
 * User: stukushin
 * Date: 16.04.13
 * Time: 13:20
 */
public class DescendantCustomDeciderImpl implements DescendantCustomDecider {
    private CustomWorkerClient customWorker;

    @Override
    public Promise<Integer> calculate(int a, int b) {
        return customWorker.sum(a, b);
    }

    public void setCustomWorker(CustomWorkerClient customWorker) {
        this.customWorker = customWorker;
    }
}
