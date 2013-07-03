package ru.taskurotta.recipes.custom.deciders;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 16.04.13
 * Time: 13:13
 */

@Decider
public interface DescendantCustomDecider {
    @Execute
    public Promise<Integer> calculate(int a, int b);
}
