package ru.taskurotta.server.config.expiration.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.config.model.ExpirationPolicy;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 *  Expiration policy which considers task to be expired after a given timeout
 */
public class TimeoutPolicy implements ExpirationPolicy {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutPolicy.class);

    public static final String TIMEOUT = "timeout";
    public static final String TIME_UNIT = "timeUnit";

    protected int timeout = -1;
    protected TimeUnit timeUnit = TimeUnit.SECONDS;

    public TimeoutPolicy(Properties props) {
        if (props!=null && !props.isEmpty()) {
            if (props.containsKey(TIMEOUT)) {
                this.timeout = Integer.valueOf(props.get(TIMEOUT).toString());
            }
            if (props.containsKey(TIME_UNIT)) {
                this.timeUnit = TimeUnit.valueOf(props.get(TIME_UNIT).toString().toUpperCase());
            }
        }
        logger.debug("TimeoutPolicy created. timeout[{}], timeUnit[{}]", this.timeout, this.timeUnit);
    }

    @Override
    public boolean readyToRecover(UUID taskId, UUID processId) {
        return true;
    }

    @Override
    public long getExpirationTime(UUID taskId, UUID processId, long forTime) {
        //forTime + fixed timeout
        return forTime + timeUnit.toMillis(timeout);
    }


}
