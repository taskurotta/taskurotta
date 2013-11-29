package ru.taskurotta.recipes.parallel.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 18.03.13
 * Time: 14:41
 */
@DeciderClient(decider = PiDecider.class)
public interface PiDeciderClient {
    @Execute
    public void calculate(long cycles, long accuracy);
}
