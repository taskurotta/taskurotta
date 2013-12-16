package ru.taskurotta.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AcceptFail annotation is used on method of {@link Decider} or {@link Worker} to mark that method may return exception.
 * Promise of this method can't be sent to workers. Deciders must check state of such promises before use.
 *
 * Created by void 26.11.13 13:28
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AcceptFail {
    Class[] type() default {};
}
