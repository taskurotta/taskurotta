package ru.taskurotta.example.calculate.worker.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.example.calculate.RandomException;
import ru.taskurotta.example.calculate.worker.Summarizer;

public class SummarizerImpl implements Summarizer {

    private static final Logger logger = LoggerFactory.getLogger(SummarizerImpl.class);
    private long sleep = -1l;
    private double errPossibility = 0.0d;

    @Override
    public Integer summarize(Integer a, Integer b) {
        logger.trace("summarize() called");
        if(RandomException.isEventHappened(errPossibility)) {
            logger.error("Summarizer: RANDOMLY FAILED!");
            throw new RandomException("Its exception time");
        }

        if(sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                logger.error("Sleep interrupted", e);
            }
        }

        Integer result = a+b;

        logger.debug("Summ result is[{}]", result);
        return result;
    }

    public void setSleep(long sleep) {
        this.sleep = sleep;
    }

    public void setErrPossibility(double errPossibility) {
        this.errPossibility = errPossibility;
    }

}
