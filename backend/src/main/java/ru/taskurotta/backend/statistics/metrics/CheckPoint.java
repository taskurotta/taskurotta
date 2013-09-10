package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: romario
 * Date: 9/5/13
 * Time: 4:15 PM
 */
public class CheckPoint {

    private String name;
    private DataListener dataListener;

    private AtomicReference<State> stateRef = new AtomicReference<>();

    private class State {
        protected volatile long counter = 0;
        protected volatile double value = 0;
    }

    public CheckPoint(String name, DataListener dataListener) {
        this.name = name;
        this.dataListener = dataListener;

        stateRef.set(new State());
    }

    public void mark(long period) {

        State newState = new State();

        while (true) {
            State oldState = stateRef.get();

            newState.counter = oldState.counter + 1;
            newState.value = ((oldState.value * oldState.counter) + period) / newState.counter;

            if (stateRef.compareAndSet(oldState, newState)) {
                break;
            }
        }
    }

    public void dump() {
        State oldState = stateRef.get();
        dataListener.handle(name, oldState.counter, oldState.value, System.currentTimeMillis());
    }
}