package ru.taskurotta.service.metrics;

import ru.taskurotta.service.metrics.handler.DataListener;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: romario
 * Date: 9/5/13
 * Time: 4:15 PM
 */
public class CheckPoint {

    private AtomicReference<State> stateRef = new AtomicReference<>();

    private static class State {
        protected volatile long counter = 0;
        protected volatile double sum = 0;
    }

    public CheckPoint() {
        stateRef.set(new State());
    }

    public void mark(long period) {

        State newState = new State();

        while (true) {
            State oldState = stateRef.get();

            newState.counter = oldState.counter + 1;
            newState.sum = oldState.sum + period;

            if (stateRef.compareAndSet(oldState, newState)) {
                break;
            }
        }
    }

    public void dumpCurrentState(DataListener dataListener, String metricName, String datasetName) {
        State state = stateRef.getAndSet(new State());//Reset state and get old value for dumping
        dataListener.handle(metricName, datasetName, state.counter, state.sum/state.counter, System.currentTimeMillis());
    }

}