package ru.taskurotta.example.calculate.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.LinearRetry;
import ru.taskurotta.exception.Retriable;

@Decider
public interface MathActionDecider {

    @Execute
    @LinearRetry(initialRetryIntervalSeconds=5, exceptionsToRetry={Retriable.class})
    public void performAction();

}
