package ru.taskurotta.recipes.multiplier;

import ru.taskurotta.core.Promise;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by void 09.07.13 19:29
 */
public class MultiplierDeciderImpl implements MultiplierDecider {

    private AtomicLong taskCount = new AtomicLong(0);
    private AtomicLong lastTime = new AtomicLong(0);
    private AtomicLong startTime = new AtomicLong(0);

    @Override
    public Promise<Integer> multiply(Integer a, Integer b) {
        if (taskCount.get() == 0) {
            long current = System.currentTimeMillis();
            lastTime.set(current);
            startTime.set(current);
        }
        long curTime = System.currentTimeMillis();
        long count = taskCount.incrementAndGet();
        if (count % 1000 == 0) {
            System.out.printf("       tasks done: %6d; time: %6.3f s; rate: %8.3f tps\n", count, 0.001 * (curTime - lastTime.get()), 1000.0D * 1000 / (curTime - lastTime.get()));
            lastTime.set(curTime);
        }
        return Promise.asPromise(a * b);
    }
}
