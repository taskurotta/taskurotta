package ru.taskurotta.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by void 22.03.13 13:05
 */
@Target(ElementType.PARAMETER)
public @interface NoWait {
}
