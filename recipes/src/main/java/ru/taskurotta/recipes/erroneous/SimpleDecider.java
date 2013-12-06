package ru.taskurotta.recipes.erroneous;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * Created by void 27.03.13 14:20
 */
@Decider
public interface SimpleDecider {

    @Execute
    public void start();

}
