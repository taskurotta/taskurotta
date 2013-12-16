package ru.taskurotta.recipes.calculate.worker.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.recipes.calculate.RandomException;
import ru.taskurotta.recipes.calculate.worker.Multiplier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

public class MultiplierImpl implements Multiplier {

    private static final Logger logger = LoggerFactory.getLogger(MultiplierImpl.class);

    private long sleep = -1l;

    private double errPossibility = 0.0d;

    private boolean varyExceptions = false;

    @Override
    public Integer multiply (Integer a, Integer b) throws Exception {
        logger.trace("multiply() called");
        if (RandomException.isEventHappened(errPossibility)) {
            logger.error("Multiplier: RANDOMLY FAILED!");
            if (varyExceptions) {
                throw RandomException.getRandomException();
            } else {
                throw new RandomException("Its multiply exception time");
            }
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

    public void setVaryExceptions(boolean varyExceptions) {
        this.varyExceptions = varyExceptions;
    }
}
