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
    private DArgSubprocessDeciderClient subDeciderClient;

    private DArgArbiter arbiter;

    @Override
    public void start() {
        arbiter.notify("start");
        Promise<String> p1 = selfAsync.getParam();
        workerClient.getNumber(p1);
        selfAsync.useParam(p1);


        Promise<String> p2 = workerClient.getParam();

        Promise<String> p3 = workerClient.getParam();
        Promise<String> subValue = subDeciderClient.getSubprocessValue(p3);

        Promise<String> resultValue = workerClient.processParams("plain param", p2, subValue, p1);

        workerClient.getNumber(p2);


        selfAsync.waitResultAndLogIt(resultValue);

    }

    @Asynchronous
    public void waitResultAndLogIt(Promise<String> resultValue) {
        arbiter.notify("waitResultAndLogIt");
        logger.info("Result getted is [{}]", resultValue);
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

    @Required
    public void setWorkerClient(DArgWorkerClient workerClient) {
        this.workerClient = workerClient;
    }

    @Required
    public void setSelfAsync(DArgDeciderImpl selfAsync) {
        this.selfAsync = selfAsync;
    }

    @Required
    public void setArbiter(DArgArbiter arbiter) {
        this.arbiter = arbiter;
    }

    @Required
    public void setSubDeciderClient(DArgSubprocessDeciderClient subDeciderClient) {
        this.subDeciderClient = subDeciderClient;
    }
}
