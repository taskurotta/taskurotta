package ru.taskurotta.dropwizard.server.pooling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ExecutorServiceFactory {

    private int threads = 1;

    public ExecutorService getFixedThreadPool() {
        return Executors.newFixedThreadPool(threads);
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

}
