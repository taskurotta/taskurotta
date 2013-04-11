package ru.taskurotta.recipes.retrypolicy.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 11.04.13
 * Time: 20:01
 */

@Decider
public interface SumDecider {
    @Execute
    public void calculate(int a, int b);
}
