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
    public void handle(String type, String name, String actorId, long value, long time) {
        if (logger.isDebugEnabled()) {
            logger.debug("METRICS: [{}] [{}] = [{}] for actor [{}], collected at [{}]", type, name, value, actorId, new Date(time));
        }
    }
}
