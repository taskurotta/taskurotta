package ru.taskurotta.recipes.custom.workers;

import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 15.04.13
 * Time: 19:07
 */

@Worker
public interface CustomWorker {
    public int sum(int a, int b);
}
