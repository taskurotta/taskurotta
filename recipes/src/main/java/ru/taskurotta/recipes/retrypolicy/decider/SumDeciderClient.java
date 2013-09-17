package ru.taskurotta.recipes.retrypolicy.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 11.04.13
 * Time: 20:02
 */

@DeciderClient(decider = SumDecider.class)
public interface SumDeciderClient {
    @Execute
    public void calculate(int a, int b);
}
