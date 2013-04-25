package ru.taskurotta.test.monkey;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by void 16.04.13 13:59
 */
@Aspect
public abstract class AbstractMonkeyAspect {
    private final static Logger log = LoggerFactory.getLogger(AbstractMonkeyAspect.class);

    private double threshold = 0.1;

    //"execution(* ru.taskurotta.backend.queue.MemoryQueueBackend.pollCommit(..))"
    @Pointcut
    public abstract void pointCut();

    @Before(value = "pointCut()", argNames = "pjp")
    public void advice(JoinPoint pjp) throws Throwable {
        log.trace("pointCut [{}]", pjp);

        if (Math.random() < threshold) {
            throw new CrazyException("Insane detected in ["+ pjp.getSignature()+"]");
        }
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
