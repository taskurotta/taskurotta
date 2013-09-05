package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: romario
 * Date: 9/5/13
 * Time: 4:15 PM
 */
public class AtomicCheckPoint implements CheckPoint {

    private String name;
    private String actorId;
    private DataListener dataListener;

    private AtomicReference<State> stateRef = new AtomicReference<>();
    private ThreadLocal<State> reusedState = new ThreadLocal<>();

    private class State {
        protected volatile long counter = 0;
        protected volatile double value = 0;
    }


    public AtomicCheckPoint(String name, String actorId, DataListener dataListener) {
        this.name = name;
        this.actorId = actorId;
        this.dataListener = dataListener;

        stateRef.set(new State());

    }

    @Override
    public void mark(long period) {

        State newState = new State();

//        State newState = reusedState.get();
//        if (newState == null) {
//            newState = new State();
//            reusedState.set(newState);
//        }

        while (true) {
            State oldState = stateRef.get();

            newState.counter = oldState.counter + 1;
            newState.value = ((oldState.value * oldState.counter) + period) / newState.counter;

            if (stateRef.compareAndSet(oldState, newState)) {
                break;
            }

        }


    }

    @Override
    public void dump() {

        State oldState = stateRef.get();
        dataListener.handle(name, actorId, oldState.counter, oldState.value, System.currentTimeMillis());
    }

}