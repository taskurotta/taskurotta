package ru.taskurotta.service.console.manager;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.model.ProfileVO;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.console.model.QueueVO;
import ru.taskurotta.service.console.model.TaskTreeVO;
import ru.taskurotta.service.queue.TaskQueueItem;
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
    public GenericPage<QueueVO> getQueuesState(int pageNumber, int pageSize);

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
    public Process getProcess(UUID processUuid);

    /**
     * @return list of method profiles marked by Profiled annotation
     */
    public List<ProfileVO> getProfilesInfo();

    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    public GenericPage<Process> listProcesses(int pageNumber, int pageSize, int status, String typeFilter);

    public TaskTreeVO getTreeForTask(UUID taskUuid, UUID processId);

    public TaskTreeVO getTreeForProcess(UUID processUuid);

    public List<Process> findProcesses(String processId, String customId);

    public List<TaskContainer> findTasks(String processId, String taskId);

    public List<QueueVO> getQueuesHovering(float periodSize);

    public Collection<TaskContainer> getRepeatedTasks(int iterationCount);

    public GenericPage<QueueStatVO> getQueuesStatInfo(int pageNumber, int pageSize, String filter);

    public int getFinishedCount(String customId);

}
