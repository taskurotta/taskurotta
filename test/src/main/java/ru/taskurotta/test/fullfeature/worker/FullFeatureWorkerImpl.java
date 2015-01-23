package ru.taskurotta.test.fullfeature.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by void 20.12.13 19:02
 */
public class FullFeatureWorkerImpl implements FullFeatureWorker {
    protected final static Logger log = LoggerFactory.getLogger(FullFeatureWorkerImpl.class);

    @Override
    public double sqr(double a) {
        //log.info("FullFeatureWorkerImpl.sqr({})", a);
        return a*a;
    }

    @Override
    public double sqrt(double a) {
        //log.info("FullFeatureWorkerImpl.sqrt({})", a);
        if (a < 0) {

            throw new IllegalArgumentException("Argument can not be less than 0: " + a);
        }
        return Math.sqrt(a);
    }
}
