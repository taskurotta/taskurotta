package ru.taskurotta.recipes.custom.deciders;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 15.04.13
 * Time: 19:06
 */

@Decider
public interface CustomDecider {
    @Execute
    public void calculate(int a, int b);
}
