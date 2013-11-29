package ru.taskurotta.recipes.recursion.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 19.03.13
 * Time: 12:46
 */
@Decider
public interface FibonacciDecider {
    @Execute
    public Promise<Integer> calculate(int n);
}
