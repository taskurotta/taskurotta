package ru.taskurotta.recipes.darg.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.darg.DArgArbiter;
import ru.taskurotta.recipes.darg.worker.DArgWorkerClient;

public class DArgDeciderImpl implements DArgDecider {
    protected final static Logger log = LoggerFactory.getLogger(DArgDeciderImpl.class);

    private static Logger logger = LoggerFactory.getLogger(DArgDeciderImpl.class);

    private DArgWorkerClient workerClient;
    private DArgDeciderImpl selfAsync;
    private DArgArbiter arbiter;

    @Override
    public void start() {
        arbiter.notify("start");
        Promise<String> p1 = selfAsync.getParam();
        Promise<String> p2 = workerClient.getParam();

        workerClient.getNumber(p1);
        workerClient.getNumber(p2);

        selfAsync.useParam(p1);

    }

    @Asynchronous
    public void useParam(Promise<String> param) {
        arbiter.notify("useParam");
        log.info("Using param: {}", param.get());
    }

    @Asynchronous
    public Promise<String> getParam() {
        arbiter.notify("getParamDecider");
        log.info("Hello, bug!");
        return Promise.asPromise("Hello, bug!");
    }

    public void setWorkerClient(DArgWorkerClient workerClient) {
        this.workerClient = workerClient;
    }

    public void setSelfAsync(DArgDeciderImpl selfAsync) {
        this.selfAsync = selfAsync;
    }

    @Required
    public void setArbiter(DArgArbiter arbiter) {
        this.arbiter = arbiter;
    }
}
