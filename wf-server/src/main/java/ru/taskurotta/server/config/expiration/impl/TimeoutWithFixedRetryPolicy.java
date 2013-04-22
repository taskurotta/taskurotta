package ru.taskurotta.server.config.expiration.impl;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TimeoutWithFixedRetryPolicy extends TimeoutPolicy {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutWithFixedRetryPolicy.class);

    public static final String RETRY = "retry";
    private Map<UUID, Integer> expirations = new ConcurrentHashMap<UUID, Integer>();

    private int retry = -1;

    public TimeoutWithFixedRetryPolicy(Properties props) {
        super(props);
        if(props!=null && !props.isEmpty()) {
            if(props.containsKey(RETRY)) {
                this.retry = Integer.valueOf(props.get(RETRY).toString());
            }
        }
        logger.debug("TimeoutWithFixedRetryPolicy created. retry[{}], timeout[{}], timeUnit[{}]", this.retry, this.timeout, this.timeUnit);
    }

    @Override
    public boolean readyToRecover(UUID uuid) {
        boolean result = true;
        if(retry > 0) {
            Integer taskRetry = expirations.get(uuid);

            if(taskRetry == null || taskRetry < retry) {
                expirations.put(uuid, Integer.valueOf(taskRetry==null?1: taskRetry.intValue()+1));
            } else {
                result = false;
                logger.error("Task[{}] expiration policy commit failed: Task has been already retried for [{}]/[{}] times", uuid, taskRetry, retry);
            }

        }
        return result;

    }

    @Override
    public long getExpirationTime(UUID taskUuid, long forTime) {
        //forTime + fixed timeout
        return forTime + timeUnit.toMillis(timeout);
    }

}
