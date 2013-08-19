package ru.taskurotta.recipes.pcollection.decider;

import ru.taskurotta.annotation.DeciderClient;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 22.07.13 16:12
 */
@DeciderClient(decider = CollectionOfPromiseDecider.class)
public interface CollectionOfPromiseDeciderClient {

    public void execute(int size);

}
