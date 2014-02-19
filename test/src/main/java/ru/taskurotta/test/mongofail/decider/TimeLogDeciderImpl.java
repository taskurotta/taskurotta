package ru.taskurotta.test.mongofail.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Execute;

import java.util.Date;

/**
 * Date: 19.02.14 13:14
 */
public class TimeLogDeciderImpl implements TimeLogDecider {

    private static final Logger logger = LoggerFactory.getLogger(TimeLogDecider.class);

    private TimeLogDeciderImpl itself;

    @Execute
    public void execute() {
        itself.logTime();
    }

    @Asynchronous
    public void logTime() {
        logger.info("Time is [{}] ms", new Date().getTime());
    }

    public void setItself(TimeLogDeciderImpl itself) {
        this.itself = itself;
    }

}
