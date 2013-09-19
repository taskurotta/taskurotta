package ru.taskurotta.recipes.wait.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Wait;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.wait.worker.WaitWorkerClient;
import ru.taskurotta.test.flow.FlowArbiter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by void 13.05.13 19:33
 */
public class WaitDeciderImpl implements WaitDecider {
    protected final static Logger log = LoggerFactory.getLogger(WaitDeciderImpl.class);

    private FlowArbiter arbiter;
    private WaitWorkerClient worker;
    private WaitDeciderImpl async;

    @Override
    public void start() {
        arbiter.notify("start");
        Collection<Promise<Integer>> data = new ArrayList<>();
        //data[0] = worker.prepare();
        for (int i = 0; i < 3; i++) {
            data.add(worker.generate());
        }
        async.waitForStart(data);
    }

    @Asynchronous
    public void waitForStart(@Wait Collection<Promise<Integer>> data) {
        arbiter.notify("waitFor");
        int result = 0;
        for (Promise<Integer> promise : data) {
            result += promise.get();
        }
        log.info("result : {}", result);
    }

    public void setWorker(WaitWorkerClient worker) {
        this.worker = worker;
    }

    public void setAsync(WaitDeciderImpl async) {
        this.async = async;
    }

    public void setArbiter(FlowArbiter arbiter) {
        this.arbiter = arbiter;
    }
}
