package ru.taskurotta.test.fat.response;

import ru.taskurotta.annotation.Worker;

/**
 * Created on 28.05.2015.
 */
@Worker
public interface FatWorker {

    Response createResponse(int size);

}
