package ru.taskurotta.core;

import ru.taskurotta.internal.core.TaskType;

/**
 * Instance of TaskTarget implementation should be immutable object.
 * TODO: Method overloading. You should use different method names (in worker and decider).
 *
 * User: stukushin
 * Date: 28.12.12
 * Time: 16:24
 */
public interface TaskTarget {
    TaskType getType();
    String getName();
    String getVersion();
    String getMethod();
}
