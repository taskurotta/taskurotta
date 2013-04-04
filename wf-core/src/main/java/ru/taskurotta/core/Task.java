package ru.taskurotta.core;

import java.util.UUID;

/**
 * Instance of Task implementation should be immutable object.
 * <p/>
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
	 * @return Target of task consumer
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
	 * @return array of arguments
	 */
	public Object[] getArgs();

    /**
     * Time in milliseconds when task should be started.
     * @return
     */
    public long getStartTime();

    /**
     * Returns number of attempts to perform the task.
     * @return
     */
    public int getNumberOfAttempts();

    /**
     * Several options of task scheduling. This field always null on tasks received from server because this information
     * not needed on client side.
     *
     * @return
     */
	public TaskOptions getTaskOptions();
}
