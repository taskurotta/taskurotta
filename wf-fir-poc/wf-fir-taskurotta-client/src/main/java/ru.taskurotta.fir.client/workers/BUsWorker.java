package ru.taskurotta.fir.client.workers;

import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 10:59
 */

@Worker
public interface BusWorker {
    public void sendPackage(String uuid, int result);
}
