package ru.taskurotta.recipes.wait.worker;

import ru.taskurotta.test.flow.FlowArbiter;

/**
 * Created by void 13.05.13 19:56
 */
public class WaitWorkerImpl implements WaitWorker {
    private FlowArbiter arbiter;

    @Override
    public int generate() {
        arbiter.notify("generate");
        return (int)Math.round(Math.random() * 10);
    }

    @Override
    public int prepare() {
        arbiter.notify("prepare");
        return (int)Math.round(Math.random() * 10);
    }

    public void setArbiter(FlowArbiter arbiter) {
        this.arbiter = arbiter;
    }
}
