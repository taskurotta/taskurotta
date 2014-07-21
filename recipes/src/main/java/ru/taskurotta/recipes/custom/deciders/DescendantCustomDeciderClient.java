package ru.taskurotta.recipes.custom.deciders;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.TaskConfig;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 16.04.13
 * Time: 13:18
 */

@DeciderClient(decider = DescendantCustomDecider.class)
public interface DescendantCustomDeciderClient {
    @Execute
    public Promise<Integer> calculate(int a, int b);

    @Execute
    public Promise<Integer> calculate(int a, int b, TaskConfig taskConfig);

    @Execute
    public Promise<Integer> calculate(int a, int b, Promise<?> ... waitFor);

    @Execute
    public Promise<Integer> calculate(int a, int b, TaskConfig taskConfig, Promise<?> ... waitFor);
}
