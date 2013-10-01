package ru.taskurotta.recipes.notification.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 15:44
 */
public class SMSWorkerImpl implements SMSWorker {
    private static final Logger logger = LoggerFactory.getLogger(SMSWorkerImpl.class);

    @Override
    public boolean send(String phoneNumber, String message) {
        logger.info(".send(phoneNumber = [{}], message = [{}])", phoneNumber, message);

        return true;
    }
}
