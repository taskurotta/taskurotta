package ru.taskurotta.bugtest.darg.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 15.07.13 17:15
 */
public class DArgWorkerImpl implements DArgWorker {
    protected final static Logger log = LoggerFactory.getLogger(DArgWorkerImpl.class);

    @Override
    public Integer getNumber(String param) {
        log.info("getNumber");
        return Integer.valueOf(1);
    }

    @Override
    public String getParam() {
        return "Hello param from worker";
    }
}
