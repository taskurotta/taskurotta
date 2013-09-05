package ru.taskurotta.backend.statistics.datalisteners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * User: stukushin
 * Date: 26.08.13
 * Time: 19:02
 */
public class LoggerDataListener implements DataListener {

    private final static Logger logger = LoggerFactory.getLogger(LoggerDataListener.class);

    @Override
    public void handle(String name, String actorId, int count, double value, long time) {
        if (logger.isInfoEnabled()) {
            logger.info("METRICS: [{}]#[{}]: value = [{}], count = [{}] collected at [{}]", name, actorId, value, count, new Date(time));
        }
    }
}
