package ru.taskurotta.recipes.idempotencykey;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.TaskConfig;

/**
 * Created by void 09.07.13 19:28
 */
@DeciderClient(decider = MultiplierDecider.class)
public interface MultiplierDeciderClient {

    @Execute
    public Promise<Integer> multiply(Integer a, Integer b);

    @Execute
    public Promise<Integer> multiply(Integer a, Integer b, TaskConfig schedulingOptions);
}
