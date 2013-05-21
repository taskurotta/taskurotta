package ru.taskurotta.console.manager;

import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.console.model.QueueVO;

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

    public List<TaskContainer> getEnqueueTasks(String queueName);

}
