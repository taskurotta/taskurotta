package ru.taskurotta.core;

import java.util.UUID;

/**
 * Instance of Task implementation should be immutable object.
 *
 * User: romario
 * Date: 12/26/12
 * Time: 12:09 PM
 */
public interface Task {

    /**
     * Unique task id.
     * Can not be null.
     */

    public UUID getId();


    /**
     * Target of task consumer.
     * Can not be null.
     *
     * @return
     */
    public TaskTarget getTarget();


    /**
     * Args an array of objects containing the values of the
     * arguments passed in the method invocation on the proxy instance,
     * or <code>null</code> if interface method takes no arguments.
     * Arguments of primitive types are wrapped in instances of the
     * appropriate primitive wrapper class, such as
     * <code>java.lang.Integer</code> or <code>java.lang.Boolean</code>.
     *
     * @return
     */
    public Object[] getArgs();
}
