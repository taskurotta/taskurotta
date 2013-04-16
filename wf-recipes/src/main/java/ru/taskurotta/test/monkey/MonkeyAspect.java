package ru.taskurotta.test.monkey;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by void 16.04.13 13:59
 */
@Aspect
public class MonkeyAspect {
    private final static Logger log = LoggerFactory.getLogger(MonkeyAspect.class);

    private double threshold = 0.1;

    @Around("execution(* ru.taskurotta.backend.queue.MemoryQueueBackend.pollCommit(..))")
    public Object advice(ProceedingJoinPoint pjp) throws Throwable {
        //log.debug("pointCut [{}]", pjp);

        if (Math.random() < threshold) {
            throw new CrazyException("Insane detected in ["+ pjp.getSignature()+"]");
        }
        return pjp.proceed();
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
