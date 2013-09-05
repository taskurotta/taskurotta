package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * User: stukushin
 * Date: 04.09.13
 * Time: 10:53
 */
public class ArrayCheckPoint implements CheckPoint {

    private String name;
    private String actorId;
    private DataListener dataListener;

    private final int size;
    private AtomicLongArray data;

    private AtomicInteger position = new AtomicInteger(0);
    private AtomicInteger lastDumpPositionAtomicInteger = new AtomicInteger(0);

    public ArrayCheckPoint(String name, String actorId, DataListener dataListener) {
        this(name, actorId, dataListener, 5000);
    }

    public ArrayCheckPoint(String name, String actorId, DataListener dataListener, int size) {
        this.name = name;
        this.actorId = actorId;
        this.dataListener = dataListener;
        this.size = size;

        this.data = new AtomicLongArray(size);
    }

    @Override
    public void mark(long period) {
        position.compareAndSet(size, 0);

        data.set(position.getAndIncrement(), period);

        position.compareAndSet(size, 0);
    }

    @Override
    public void dump() {
        int currentPosition = position.get();

        if (lastDumpPositionAtomicInteger.get() == currentPosition) {
            dataListener.handle(name, actorId, 0, 0, System.currentTimeMillis());
            return;
        }

        int lastPosition = lastDumpPositionAtomicInteger.getAndSet(currentPosition);

        int count = currentPosition < lastPosition ? size - lastPosition + currentPosition : currentPosition - lastPosition;

        long sum = 0;

        if (lastPosition < currentPosition) {
            for (int i = lastPosition; i < currentPosition; i++) {
                sum += data.get(i);
            }
        } else {
            for (int i = lastPosition; i < size; i++) {
                sum += data.get(i);
            }

            for (int i = 0; i < currentPosition; i++) {
                sum += data.get(i);
            }
        }

        dataListener.handle(name, actorId, count, (double) (sum / count), System.currentTimeMillis());
    }
}
