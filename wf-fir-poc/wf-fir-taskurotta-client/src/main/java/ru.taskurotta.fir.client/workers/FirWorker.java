package ru.taskurotta.fir.client.workers;

import ru.taskurotta.annotation.Worker;

/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 10:40
 */

@Worker
public interface FirWorker {
    public void request(int a, int b);
    public int response();
}
