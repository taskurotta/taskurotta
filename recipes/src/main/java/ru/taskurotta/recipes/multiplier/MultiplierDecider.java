package ru.taskurotta.recipes.multiplier;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.Promise;

/**
 * Created by void 09.07.13 19:26
 */
@Decider
public interface MultiplierDecider {
    @Execute
    public Promise<Integer> multiply(Integer a, Integer b);
}
