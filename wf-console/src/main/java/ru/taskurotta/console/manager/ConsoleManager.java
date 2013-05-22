package ru.taskurotta.console.manager;

import ru.taskurotta.console.model.QueueVO;
import ru.taskurotta.console.model.QueuedTaskVO;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.List;
import java.util.UUID;

/**
 * Manager interface, providing aggregated info gathered from concrete retrievers implementations
 * User: dimadin
 * Date: 17.05.13 16:03
 */
public interface ConsoleManager {

    public List<QueueVO> getQueuesState();

    public List<TaskContainer> getProcessTasks(UUID processUuid);

    public List<QueuedTaskVO> getEnqueueTasks(String queueName);

}
