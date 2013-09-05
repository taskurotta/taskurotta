package ru.taskurotta.backend.statistics.metrics;

import com.google.common.util.concurrent.AtomicDouble;
import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.concurrent.atomic.AtomicLong;

/**
 * User: stukushin
 * Date: 05.09.13
 * Time: 15:34
 */
public class MeanCheckPoint implements CheckPoint {

    private String name;
    private String actorId;
    private DataListener dataListener;

    private AtomicLong counter;
    private AtomicDouble value;

    public MeanCheckPoint(String name, String actorId, DataListener dataListener) {
        this.name = name;
        this.actorId = actorId;
        this.dataListener = dataListener;

        this.counter = new AtomicLong();
        this.value = new AtomicDouble();
    }

    @Override
    public void mark(long period) {
        value.set(((value.get() * counter.get()) + period) / counter.incrementAndGet());
    }

    @Override
    public void dump() {
        dataListener.handle(name, actorId, counter.get(), value.get(), System.currentTimeMillis());
        counter.set(0);
        value.set(0);
    }
}
