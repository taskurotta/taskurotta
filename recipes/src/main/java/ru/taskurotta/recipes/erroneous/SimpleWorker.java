package ru.taskurotta.recipes.erroneous;

import ru.taskurotta.annotation.Worker;

/**
 * Created by void 18.10.13 18:25
 */
@Worker
public interface SimpleWorker {

    int createNumber();
    int print(int number);

}
