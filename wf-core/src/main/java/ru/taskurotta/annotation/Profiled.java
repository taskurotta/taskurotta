package ru.taskurotta.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marker for public methods to be profiled via AOP
 * User: dimadin
 * Date: 27.05.13 14:20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Profiled {

    /**
     * Profile name, "className#methodName" by default
     */
    public String name() default "";

    /**
     * Profile method call only if result value is not null
     */
    public boolean notNull() default false;

}
