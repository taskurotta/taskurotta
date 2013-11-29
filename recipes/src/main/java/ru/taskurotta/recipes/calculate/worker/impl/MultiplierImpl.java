package ru.taskurotta.recipes.calculate.worker.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.recipes.calculate.RandomException;
import ru.taskurotta.recipes.calculate.worker.Multiplier;

public class MultiplierImpl implements Multiplier {

    private static final Logger logger = LoggerFactory.getLogger(MultiplierImpl.class);

    private long sleep = -1l;

    private double errPossibility = 0.0d;

    @Override
    public Integer multiply(Integer a, Integer b) {
        logger.trace("multiply() called");
        if (RandomException.isEventHappened(errPossibility)) {
            logger.error("Multiplier: RANDOMLY FAILED!");
            throw new RandomException("Its multiply exception time");
        }

        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                logger.error("Sleep interrupted", e);
            }
        }

        Integer result =  a * b;

        logger.debug("Multiplication result is[{}]", result);
        return result;
    }

    public void init() {
        logger.info("MultiplierImpl initialized with errPossibility[{}], sleep[{}] ", errPossibility, sleep);
    }

    public void setSleep(long sleep) {
        this.sleep = sleep;
    }

    public void setErrPossibility(double errPossibility) {
        this.errPossibility = errPossibility;
    }

}
