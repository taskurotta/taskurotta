package ru.taskurotta.test.profiler;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by void 17.07.13 13:52
 */
@Aspect
public abstract class AbstractProfilerAspect {
    private static final Logger log = LoggerFactory.getLogger(AbstractProfilerAspect.class);
    private static final Map<String, Pair<Integer, Long>> counters = new HashMap<>();

    private static final int updatesForStat = 100;
    @Pointcut
    public abstract void pointCut();

    @Around(value = "pointCut()", argNames = "joinPoint")
    public Object advice(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("advice: {}", joinPoint);
        long start = System.nanoTime();
        Object result = joinPoint.proceed();
        long end = System.nanoTime();
        long spentTime = end - start;

        updateCounter(joinPoint.getSignature().toString(), spentTime);

        return result;
    }

    private static void updateCounter(String key, long value) {
        Pair<Integer, Long> counter;
        int count;
        long time;
        synchronized (counters) {
            counter = getCounter(key);
            count = counter.getLeft() + 1;
            time = counter.getRight() + value;

            if (count % updatesForStat == 0) {
                System.out.printf("       key: [%s]; tasks: %6d; time: %6.3f ms; time for one: %6.3f ms\n", key, count, 1e-6 * time, 1e-6 * time / updatesForStat);
                counter.setLeft(0);
                counter.setRight(0L);
            } else {
                counter.setLeft(count);
                counter.setRight(time);
            }

            counters.put(key, counter);
        }
    }

    private static Pair<Integer, Long> getCounter(String key) {
        Pair<Integer, Long> counter = counters.get(key);
        return counter == null ? new Pair<>(0, 0L) : counter;
    }
}
