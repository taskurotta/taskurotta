package ru.taskurotta.backend.statistics.datalisteners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * User: stukushin
 * Date: 10.09.13
 * Time: 16:20
 */
public class LoggerActorDataListener extends ActorDataListener {

    private static final Logger logger = LoggerFactory.getLogger(LoggerActorDataListener.class);

    private String actorId;

    public LoggerActorDataListener(String actorId) {
        this.actorId = actorId;
    }

    @Override
    public void handle(String name, long count, double value, long time) {
        super.handle(name, count, value, time);

        if (logger.isInfoEnabled()) {
            logger.info("METRICS: [{}]#[{}]: value = [{}], count = [{}] collected at [{}]", name, actorId, value, count, new Date(time));
        }
    }
}
