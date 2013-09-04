package ru.taskurotta.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: jedy
 * User: romario
 * Date: 04.12.12 18:43
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DeciderClient {


    Class decider() default Class.class;

}
