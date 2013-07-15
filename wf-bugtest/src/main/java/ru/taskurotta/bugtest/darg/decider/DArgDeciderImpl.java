package ru.taskurotta.bugtest.darg.decider;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.bugtest.darg.worker.DArgWorkerClient;
import ru.taskurotta.core.Promise;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 15.07.13 17:14
 */
public class DArgDeciderImpl implements DArgDecider {

    private DArgWorkerClient workerClient;
    private DArgDeciderImpl selfAsync;

    @Override
    public void start(String arg) {

        Promise<String> param = selfAsync.getParam();

        Promise <Integer> result = workerClient.getNumber(param);

        selfAsync.useParam(param);

    }

    @Asynchronous
    public void useParam(Promise<String> param) {
        System.out.println("Using param: " + param.get());
    }

    @Asynchronous
    public Promise<String> getParam() {
        return Promise.asPromise("Hello, bug!");
    }

    public void setWorkerClient(DArgWorkerClient workerClient) {
        this.workerClient = workerClient;
    }

    public void setSelfAsync(DArgDeciderImpl selfAsync) {
        this.selfAsync = selfAsync;
    }
}
