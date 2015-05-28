package ru.taskurotta.test.fat.response;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * Created on 28.05.2015.
 */
@Decider(version = "2.0")
public interface FatDecider {

    @Execute
    void start(int size);

}
