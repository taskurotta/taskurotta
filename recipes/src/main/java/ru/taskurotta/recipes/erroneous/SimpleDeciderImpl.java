package ru.taskurotta.recipes.erroneous;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Fail;
import ru.taskurotta.core.Promise;

/**
 * Created by void 18.10.13 18:24
 */
public class SimpleDeciderImpl implements SimpleDecider {
    protected final static Logger log = LoggerFactory.getLogger(SimpleDeciderImpl.class);
    private SimpleWorkerClient worker;
    private SimpleDeciderImpl async;

    @Override
    public void start() {
        log.info("start");

        Promise<Integer> number1 = worker.createNumber();
        async.print(number1);

        log.info("start done");
    }

    @Asynchronous
    public void print(Promise<Integer> promise) {
        if (promise.hasFail()) {
            log.info("got fail: ["+ promise.getFail()+ "]");
        } else {
            Integer integer = promise.get();
            log.info("got number: {}", integer);
        }

        // another style of Fail-handling
        try {
            Integer integer = promise.get();
            log.info("got number: {}", integer);
        } catch (Fail fail) {
            log.info("got exception: "+ fail.getMessage(), fail);
        }
    }

    public void setWorker(SimpleWorkerClient worker) {
        this.worker = worker;
    }

    public void setAsync(SimpleDeciderImpl async) {
        this.async = async;
    }
}
