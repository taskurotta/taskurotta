package ru.taskurotta.recipes.multiplier;

import ru.taskurotta.core.Promise;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by void 09.07.13 19:29
 */
public class MultiplierDeciderImpl implements MultiplierDecider {

    @Override
    public Promise<Integer> multiply(Integer a, Integer b) {
        return Promise.asPromise(a * b);
    }
}
