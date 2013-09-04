package ru.taskurotta.recipes.darg.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.darg.DArgArbiter;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 17.07.13 16:18
 */
public class DArgSubprocessDeciderImpl implements DArgSubprocessDecider {

    private static final Logger logger = LoggerFactory.getLogger(DArgSubprocessDeciderImpl.class);

    private DArgArbiter arbiter;

    @Override
    public Promise<String> getSubprocessValue(String someParam) {
        arbiter.notify("getSubprocessValue");
        logger.info("Subprocess started");
        return Promise.asPromise("subprocess value");
    }

    @Required
    public void setArbiter(DArgArbiter arbiter) {
        this.arbiter = arbiter;
    }
}
