package ru.taskurotta.bugtest.darg.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.bugtest.darg.worker.DArgWorkerClient;
import ru.taskurotta.core.Promise;

public class DArgDeciderImpl implements DArgDecider {
    protected final static Logger log = LoggerFactory.getLogger(DArgDeciderImpl.class);

    private static Logger logger = LoggerFactory.getLogger(DArgDeciderImpl.class);
    private DArgWorkerClient workerClient;
    private DArgDeciderImpl selfAsync;

    @Override
    public void start() {

        Promise<String> p1 = selfAsync.getParam();
        Promise<String> p2 = workerClient.getParam();

        workerClient.getNumber(p1);
        workerClient.getNumber(p2);

        selfAsync.useParam(p1);

    }

    @Asynchronous
    public void useParam(Promise<String> param) {
        log.info("Using param: {}", param.get());
    }

    @Asynchronous
    public Promise<String> getParam() {
        log.info("Hello, bug!");
        return Promise.asPromise("Hello, bug!");
    }

    public void setWorkerClient(DArgWorkerClient workerClient) {
        this.workerClient = workerClient;
    }

    public void setSelfAsync(DArgDeciderImpl selfAsync) {
        this.selfAsync = selfAsync;
    }
}
