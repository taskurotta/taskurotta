package ru.taskurotta.dropwizard.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.util.DuplicationErrorSuppressor;

/**
 */
public abstract class TaskServerAbstractResource {

    private static final Logger logger = LoggerFactory.getLogger(TaskPollerResource.class);

    private DuplicationErrorSuppressor duplicationErrorSuppressor = new DuplicationErrorSuppressor(60000L, false);

    protected void logError(String msg, Throwable ex) {
        if (!duplicationErrorSuppressor.isLastErrorEqualsTo(msg, ex)) {
            logger.error(msg, ex);
        }
    }

}
