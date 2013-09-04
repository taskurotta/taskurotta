package ru.taskurotta.recipes.darg.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.recipes.darg.DArgArbiter;

/**
 * User: dimadin
 * Date: 15.07.13 17:15
 */
public class DArgWorkerImpl implements DArgWorker {

    protected final static Logger log = LoggerFactory.getLogger(DArgWorkerImpl.class);

    private DArgArbiter arbiter;

    public Integer getNumber(String param) {
        arbiter.notify("getNumberWorker");
        log.info("getNumber called with param [{}]");
        return Integer.valueOf(1);
    }

    @Override
    public String getParam() {
        arbiter.notify("getParamWorker");
        return "Hello param from worker";
    }

    @Override
    public String processParams(String p1, String p2, String p3, String p4) {
        log.info("Params getted are: {}, {}, {}, {}", p1, p2, p3, p4);
        arbiter.notify("processParams");
        return p1+p2+p3+p4;
    }

    @Required
    public void setArbiter(DArgArbiter arbiter) {
        this.arbiter = arbiter;
    }
}
