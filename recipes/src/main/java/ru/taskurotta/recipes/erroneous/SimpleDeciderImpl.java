package ru.taskurotta.recipes.erroneous;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.Promise;

/**
 * Created by void 18.10.13 18:24
 */
public class SimpleDeciderImpl implements SimpleDecider {
    protected final static Logger log = LoggerFactory.getLogger(SimpleDeciderImpl.class);
    private SimpleWorkerClient worker;

    @Override
    public void start() {
        log.info("start");

        Promise<Integer> number1 = worker.createNumber();
        Promise<Integer> number2 = worker.print(number1);

        log.info("start done");
    }

    public void setWorker(SimpleWorkerClient worker) {
        this.worker = worker;
    }
}
