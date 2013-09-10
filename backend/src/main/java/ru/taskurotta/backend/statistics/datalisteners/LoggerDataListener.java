package ru.taskurotta.backend.statistics.datalisteners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * User: stukushin
 * Date: 26.08.13
 * Time: 19:02
 */
public class LoggerDataListener extends AbstractDataListener {

    private final static Logger logger = LoggerFactory.getLogger(LoggerDataListener.class);

    public void handle(String name, long count, double mean, long time) {
        super.handle(name, count, mean, time);

        if (logger.isInfoEnabled()) {
            logger.info("METRICS: [{}]: mean = [{}], count = [{}] collected at [{}]", name, mean, count, new Date(time));
        }
    }
}
