package ru.taskurotta.recipes.parallel.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 18.03.13
 * Time: 14:39
 */
@Decider
public interface PiDecider {
    @Execute
    public void calculate(long cycles, long accuracy);
}
