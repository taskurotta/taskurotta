package ru.taskurotta.recipes.parallel.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.parallel.workers.PiWorkerClient;

/**
 * User: stukushin
 * Date: 18.03.13
 * Time: 14:42
 * <p/>
 * use http://en.wikipedia.org/wiki/Leibniz_formula_for_%CF%80 for calculating PI
 */
public class PiDeciderImpl implements PiDecider {

    private static final Logger logger = LoggerFactory.getLogger(PiDeciderImpl.class);

    private PiWorkerClient piWorker;
    private PiDeciderImpl asynchronous;

    @Override
    public void calculate(long cycles, long accuracy) {
        Promise<Double> pi = Promise.asPromise(0d);

        for (long i = 0; i < cycles; i++) {
            Promise<Double> partPi = piWorker.calculate(i, accuracy);
            pi = asynchronous.result(pi, partPi);
        }

        asynchronous.show(pi, cycles, accuracy);
    }

    @Asynchronous
    public Promise<Double> result(Promise<Double> pi, Promise<Double> partPi) {
        return Promise.asPromise(pi.get() + partPi.get());
    }

    @Asynchronous
    public void show(Promise<Double> pi, long cycles, long accuracy) {
        logger.info("Calculating Pi: " + pi.get() + " by " + cycles + " cycles with " + accuracy + " accuracy");
        System.exit(0);
    }

    public void setPiWorker(PiWorkerClient piWorker) {
        this.piWorker = piWorker;
    }

    public void setAsynchronous(PiDeciderImpl asynchronous) {
        this.asynchronous = asynchronous;
    }
}
