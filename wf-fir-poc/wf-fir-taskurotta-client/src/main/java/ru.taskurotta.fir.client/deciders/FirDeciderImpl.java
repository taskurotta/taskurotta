package ru.taskurotta.fir.client.deciders;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.fir.client.workers.client.BusWorkerClient;
import ru.taskurotta.fir.client.workers.client.FirWorkerClient;


/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 11:18
 */
public class FirDeciderImpl implements FirDecider {
    private FirWorkerClient firWorker;
    private BusWorkerClient busWorker;
    private FirDeciderImpl asynchronous;

    @Override
    public void start(String uuid, int a, int b) {
        firWorker.request(a, b);
        Promise<Integer> result = firWorker.response();
        asynchronous.waitResult(uuid, result);
    }

    @Asynchronous
    public void waitResult(String uuid, Promise<Integer> result) {
        busWorker.sendPackage(uuid, result.get());
    }

    public void setFirWorker(FirWorkerClient firWorker) {
        this.firWorker = firWorker;
    }

    public void setBusWorker(BusWorkerClient busWorker) {
        this.busWorker = busWorker;
    }

    public void setAsynchronous(FirDeciderImpl asynchronous) {
        this.asynchronous = asynchronous;
    }
}
