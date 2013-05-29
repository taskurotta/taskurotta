package ru.taskurotta.backend.console.manager;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.model.ProfileVO;
import ru.taskurotta.backend.console.model.QueueVO;
import ru.taskurotta.backend.console.model.QueuedTaskVO;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * Manager interface, providing aggregated info gathered from concrete retrievers implementations
 * User: dimadin
 * Date: 17.05.13 16:03
 */
public interface ConsoleManager {

    public GenericPage<QueueVO> getQueuesState(int pageNumber, int pageSise);

    public List<TaskContainer> getProcessTasks(UUID processUuid);

    public GenericPage<QueuedTaskVO> getEnqueueTasks(String queueName, int pageNum, int pageSize);

    public TaskContainer getTask(UUID taskId);

    public ProcessVO getProcess(UUID processUuid);

    public List<ProfileVO> getProfilesInfo();

}
