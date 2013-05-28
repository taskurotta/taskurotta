package ru.taskurotta.backend.profile;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Profiled;

/**
 * Abstract aspect, intercepting @Profile annotated public methods
 * User: dimadin
 * Date: 27.05.13 14:23
 */
@Aspect
public abstract class AbstractAspectProfiler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut(value="within(ru.taskurotta..*) && execution(public * *(..))")
    public void anyPublicMethod() { }

    @Around("anyPublicMethod() && @annotation(profiled)")
    public Object logAction(ProceedingJoinPoint pjp, Profiled profiled) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        try {
            result = pjp.proceed();
            return result;
        } finally {
            String name = profiled.name().trim().length()>0? profiled.name(): getProfileName(pjp);
            if(!(profiled.notNull() && result==null)) {
                addData(name, System.currentTimeMillis() - start);
            }
        }
    }

    public static String getProfileName(ProceedingJoinPoint pjp) {
        return pjp.getTarget().getClass().getName() + "#" + pjp.getSignature().getName();
    }

    public abstract void addData(String profileName, long time);

}
