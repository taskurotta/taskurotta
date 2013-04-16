package ru.taskurotta.recipes.custom.deciders;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.client.DeciderClientProvider;

/**
 * User: stukushin
 * Date: 15.04.13
 * Time: 19:11
 */

@DeciderClient(decider = CustomDecider.class)
public interface CustomDeciderClient {
    @Execute
    public void calculate(int a, int b);
}
