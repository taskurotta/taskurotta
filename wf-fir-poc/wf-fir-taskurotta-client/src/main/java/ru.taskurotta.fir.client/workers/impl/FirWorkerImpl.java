package ru.taskurotta.fir.client.workers.impl;

import ru.taskurotta.fir.client.workers.FirWorker;

/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 11:03
 */
public class FirWorkerImpl implements FirWorker {

    private int a;
    private int b;

    @Override
    public void request(int a, int b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int response() {
        return a + b;
    }
}
