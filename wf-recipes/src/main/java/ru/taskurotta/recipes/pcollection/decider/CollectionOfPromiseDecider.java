package ru.taskurotta.recipes.pcollection.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

/**
 * User: dimadin
 * Date: 22.07.13 11:35
 */
@Decider
public interface CollectionOfPromiseDecider {

    @Execute
    public void execute(int size);

}
