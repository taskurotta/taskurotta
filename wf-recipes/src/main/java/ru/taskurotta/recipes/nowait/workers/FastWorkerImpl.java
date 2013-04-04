package ru.taskurotta.recipes.nowait.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.test.FlowArbiter;

/**
 * Created by void 27.03.13 19:40
 */
public class FastWorkerImpl implements FastWorker {
    protected final static Logger log = LoggerFactory.getLogger(FastWorkerImpl.class);

    private FlowArbiter arbiter;

    @Override
    public int taskB() {
        log.info("taskB()");

        arbiter.notify("taskB");

        log.info("taskB done");
        return 1;
    }

    @Override
    public int taskC() {
        log.info("taskC()");
        arbiter.notify("taskC");
        return 2;
    }

    @Override
    public int taskE(int b) {
        log.info("taskE({})", b);
        arbiter.notify("taskE");
        return 3;
    }

    public void setArbiter(FlowArbiter arbiter) {
        this.arbiter = arbiter;
    }
}
