package ru.taskurotta.recipes.calculate.decider;

import ru.taskurotta.annotation.DeciderClient;


@DeciderClient(decider = MathActionDecider.class)
public interface MathActionDeciderClient {

    public void performAction();

}
