package ru.taskurotta.example.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 15:39
 */
public class EmailWorkerImpl implements EmailWorker {

    private static final Logger logger = LoggerFactory.getLogger(EmailWorkerImpl.class);

    @Override
    public boolean send(String email, String message) {
        logger.info(".send(email = [{}], message = [{}])", email, message);

        return true;
    }
}
