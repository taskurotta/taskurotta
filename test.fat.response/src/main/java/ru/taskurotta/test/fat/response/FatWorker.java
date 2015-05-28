package ru.taskurotta.test.fat.response;

import ru.taskurotta.annotation.Worker;

/**
 * Created on 28.05.2015.
 */
@Worker(version = "2.0")
public interface FatWorker {

    byte[] createResponse(int size) throws Exception;

}
