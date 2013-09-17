package ru.taskurotta.recipes.delayed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.Promise;

/**
 * Created by void 09.07.13 19:29
 */
public class MultiplierDeciderImpl implements MultiplierDecider {
    private final static Logger log = LoggerFactory.getLogger(MultiplierDecider.class);

    @Override
    public Promise<Integer> multiply(Integer a, Integer b) {
        log.info("ready to multiply {} * {}", a, b);
        return Promise.asPromise(a * b);
    }
}
