package ru.taskurotta.recipes.calculate.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.LinearRetry;
import ru.taskurotta.exception.server.ServerException;

@Decider
public interface MathActionDecider {

    @Execute
    @LinearRetry(initialRetryIntervalSeconds=5, exceptionsToRetry={ServerException.class})
    public void performAction();

}
