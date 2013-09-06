package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * User: stukushin
 * Date: 04.09.13
 * Time: 10:53
 */
public class ArrayCheckPoint2 implements CheckPoint {

    private String name;
    private String actorId;
    private DataListener dataListener;

    private final int size;
    private long[] data;

    private AtomicInteger position = new AtomicInteger(0);
    private AtomicInteger lastDumpPositionAtomicInteger = new AtomicInteger(0);

    public ArrayCheckPoint2(String name, String actorId, DataListener dataListener) {
        this(name, actorId, dataListener, 101000);
    }

    public ArrayCheckPoint2(String name, String actorId, DataListener dataListener, int size) {
        this.name = name;
        this.actorId = actorId;
        this.dataListener = dataListener;
        this.size = size;

        this.data = new long[size];
    }

    @Override
    public void mark(long period) {

		while (true) {
			int current = position.get();
			int i = current % size;
			if (position.compareAndSet(current, current+1)) {
				data[i] = period;
				break;
			}
		}
    }

    @Override
    public void dump() {
        int currentPosition = position.get();
		int lastPosition = lastDumpPositionAtomicInteger.getAndSet(currentPosition);

        if (lastPosition == currentPosition) {
            dataListener.handle(name, actorId, 0, 0, System.currentTimeMillis());
            return;
        }

        int count = currentPosition - lastPosition;
		if (count > size) {
			count = size;
		}

        long sum = 0;

        int realPosition = currentPosition % size;
		for (int i=0; i < count; i++) {
			sum += data[realPosition--];
			if (realPosition < 0) {
				realPosition = size - 1;
			}
		}

        dataListener.handle(name, actorId, count, (double)sum / count, System.currentTimeMillis());
    }
}
