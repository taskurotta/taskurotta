package ru.taskurotta.recipes.calculate.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.core.TaskConfig;


@DeciderClient(decider = MathActionDecider.class)
public interface MathActionDeciderClient {

    public void performAction();

    public void performAction(TaskConfig cfg);

}
