package ru.taskurotta.recipes.recursion.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 19.03.13
 * Time: 12:47
 */
@DeciderClient(decider = FibonacciDecider.class)
public interface FibonacciDeciderClient {
    @Execute
    public Promise<Integer> calculate(int n);
}
