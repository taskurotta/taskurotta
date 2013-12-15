package ru.taskurotta.service.statistics.datalistener.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * User: stukushin
 * Date: 10.09.13
 * Time: 16:20
 */
public class LoggerActorDataListener extends AbstractDataListener {

    private static final Logger logger = LoggerFactory.getLogger(LoggerDataListener.class);

    private String actorId;

    public LoggerActorDataListener(String actorId) {
        this.actorId = actorId;
    }

    public void handle(String metricName, String datasetName, long count, double mean, long time) {
        super.handle(metricName, datasetName, count, mean, time);

        if (logger.isInfoEnabled()) {
            logger.info("METRICS: [{}]#[{}]: mean = [{}], count = [{}] collected at [{}]", metricName, actorId, mean, count, new Date(time));
        }
    }
}
