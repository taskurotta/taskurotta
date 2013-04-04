package ru.taskurotta.recipes.nowait.workers;

import ru.taskurotta.annotation.Worker;

/**
 * Created by void 27.03.13 19:01
 */
@Worker
public interface FastWorker {
    public int taskB();

    public int taskC();

    public int taskE(int b);
}
