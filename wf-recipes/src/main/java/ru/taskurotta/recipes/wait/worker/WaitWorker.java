package ru.taskurotta.recipes.wait.worker;

import ru.taskurotta.annotation.Worker;

/**
 * Created by void 27.03.13 19:01
 */
@Worker
public interface WaitWorker {

    public int generate();

    public int prepare();

}
