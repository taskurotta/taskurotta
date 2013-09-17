package ru.taskurotta.recipes.wait.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

/**
 * Created by void 27.03.13 17:04
 */
@DeciderClient(decider = WaitDecider.class)
public interface WaitDeciderClient {

    @Execute
    public void start();

}
