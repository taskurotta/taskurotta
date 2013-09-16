package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: romario
 * Date: 9/5/13
 * Time: 4:15 PM
 */
public class CheckPoint {

    private AtomicReference<State> stateRef = new AtomicReference<>();

    private class State {
        protected volatile long counter = 0;
        protected volatile double mean = 0;
    }

    public CheckPoint() {
        stateRef.set(new State());
    }

    public void mark(long period) {

        State newState = new State();

        while (true) {
            State oldState = stateRef.get();

            newState.counter = oldState.counter + 1;
            newState.mean = oldState.mean + (period - oldState.mean) / newState.counter;

            if (stateRef.compareAndSet(oldState, newState)) {
                break;
            }
        }
    }

    public void dumpCurrentState(DataListener dataListener, String metricName, String datasetName) {
        State state = stateRef.getAndSet(new State());//Reset state and get old value for dumping
        dataListener.handle(metricName, datasetName, state.counter, state.mean, System.currentTimeMillis());
    }

}