package ru.taskurotta.test.fat.response;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * Created on 28.05.2015.
 */
@Decider
public interface FatDecider {

    @Execute
    void start(int size);

}
