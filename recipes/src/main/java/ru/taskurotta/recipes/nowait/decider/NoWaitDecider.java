package ru.taskurotta.recipes.nowait.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * Created by void 27.03.13 14:20
 */
@Decider
public interface NoWaitDecider {

    @Execute
    public void start();

}
