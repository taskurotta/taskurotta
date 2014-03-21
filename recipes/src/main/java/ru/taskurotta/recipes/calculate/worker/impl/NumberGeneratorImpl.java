package ru.taskurotta.recipes.calculate.worker.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.recipes.calculate.RandomException;
import ru.taskurotta.recipes.calculate.worker.NumberGenerator;

public class NumberGeneratorImpl implements NumberGenerator {

    private int maxNumber = 20;

    private static final Logger logger = LoggerFactory.getLogger(NumberGeneratorImpl.class);

    private long sleep = -1l;

    private double errPossibility = 0.0d;

    private boolean varyExceptions = false;

    @Override
    public Integer getNumber() throws Exception {
        logger.trace("getNumber() called");

        if (RandomException.isEventHappened(errPossibility)) {
            logger.error("NumberGenerator: RANDOMLY FAILED!");
            if (varyExceptions) {
                throw RandomException.getRandomException();
            } else {
                throw new RandomException("Its multiply exception time");
            }
        }

        if (sleep > 0) {

            logger.info("Sleep for [{}] ms", sleep);

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                logger.error("Sleep interrupted", e);
            }
        }

        Integer result =  Double.valueOf(Math.floor(Math.random() * (maxNumber+1))).intValue();
        if (result == 0) {
            result = getNumber();
        }

        logger.debug("Generated number [{}]", result);
        return result;
    }

    public void init() {
        logger.info("NumberGeneratorImpl initialized with errPossibility[{}], sleep[{}] ", errPossibility, sleep);
    }

    public void setMaxNumber(int maxNumber) {
        this.maxNumber = maxNumber;
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
