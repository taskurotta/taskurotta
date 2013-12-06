package ru.taskurotta.recipes.erroneous;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by void 18.10.13 18:29
 */
public class SimpleWorkerImpl implements SimpleWorker {
    protected final static Logger log = LoggerFactory.getLogger(SimpleDeciderImpl.class);
    private boolean iThinkItIsBadDayForNumbers = SimpleWorker.class.isInterface();

    @Override
    public int createNumber() {
        log.info("create numbers");
        if (iThinkItIsBadDayForNumbers) {
            throw new RuntimeException("I think it is bad day for numbers");
        }
        return (int) Math.round(10 * Math.random());
    }

    @Override
    public int print(int number) {
        log.info("print number: "+ number);
        System.out.println("got number: " + number);
        return number;
    }
}
