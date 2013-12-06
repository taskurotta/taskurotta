package ru.taskurotta.recipes.erroneous;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

/**
 * Created by void 18.10.13 18:22
 */
@DeciderClient(decider = SimpleDecider.class)
public interface SimpleDeciderClient {

    @Execute
    public void start();

}
