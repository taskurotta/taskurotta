package ru.taskurotta.restarter.workers;

import ru.taskurotta.annotation.Worker;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.List;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 17:19
 */
@Worker
public interface Restarter {
    public void restart(List<TaskContainer> taskContainers);
}
