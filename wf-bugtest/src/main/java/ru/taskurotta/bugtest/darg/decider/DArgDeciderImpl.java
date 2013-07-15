package ru.taskurotta.bugtest.darg.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.bugtest.darg.worker.DArgWorkerClient;
import ru.taskurotta.core.Promise;

public class DArgDeciderImpl implements DArgDecider {

    private static Logger logger = LoggerFactory.getLogger(DArgDeciderImpl.class);
    private DArgWorkerClient workerClient;
    private DArgDeciderImpl selfAsync;

    @Override
    public void start() {
        logger.info("Start");
        Promise<String> param = selfAsync.getParam();

        workerClient.getNumber(param);

    }

    @Asynchronous
    public Promise<String> getParam() {
        logger.info("getParam");
        return Promise.asPromise("Hello, bug!");
    }

    public void setWorkerClient(DArgWorkerClient workerClient) {
        this.workerClient = workerClient;
    }

    public void setSelfAsync(DArgDeciderImpl selfAsync) {
        this.selfAsync = selfAsync;
    }
}
