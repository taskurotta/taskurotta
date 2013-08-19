package ru.taskurotta.backend.console.manager;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.model.ProfileVO;
import ru.taskurotta.backend.console.model.QueueVO;
import ru.taskurotta.backend.console.model.TaskTreeVO;
import ru.taskurotta.backend.queue.TaskQueueItem;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Manager interface, providing aggregated info gathered from concrete retrievers implementations
 * User: dimadin
 * Date: 17.05.13 16:03
 */
public interface ConsoleManager {

    /**
     * @return paginated view for current state of task queues
     */
    public GenericPage<QueueVO> getQueuesState(int pageNumber, int pageSise);

    /**
     * @return list of all existing TaskContainers for a given process
     */
    public Collection<TaskContainer> getProcessTasks(UUID processUuid);

    /**
     * @return paginated view of a given queue content
     */
    public GenericPage<TaskQueueItem> getEnqueueTasks(String queueName, int pageNum, int pageSize);

    /**
     * @return TaskContainer for a given guid or null if task not found
     */
    public TaskContainer getTask(UUID taskId, UUID processId);

    /**
     * @return DecisionContainer for a given guid or null if decision not found
     */
    public DecisionContainer getDecision(UUID taskId, UUID processId);

    /**
     * @return process representation object for a given guid or null if process not found
     */
    public ProcessVO getProcess(UUID processUuid);

    /**
     * @return list of method profiles marked by Profiled annotation
     */
    public List<ProfileVO> getProfilesInfo();

    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize);

    public TaskTreeVO getTreeForTask(UUID taskUuid, UUID processId);

    public TaskTreeVO getTreeForProcess(UUID processUuid);

    public List<ProcessVO> findProcesses(String type, String id);

    public List<QueueVO> getQueuesHovering(float periodSize);

    public Collection<TaskContainer> getRepeatedTasks(int iterationCount);

    public void blockActor(String actorId);

    public void unblockActor(String actorId);
}
