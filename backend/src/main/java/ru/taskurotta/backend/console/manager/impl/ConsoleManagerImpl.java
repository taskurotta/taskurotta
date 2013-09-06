package ru.taskurotta.backend.console.manager.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ru.taskurotta.backend.console.manager.ConsoleManager;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.model.ProfileVO;
import ru.taskurotta.backend.console.model.QueueVO;
import ru.taskurotta.backend.console.model.TaskTreeVO;
import ru.taskurotta.backend.console.retriever.CheckpointInfoRetriever;
import ru.taskurotta.backend.console.retriever.ConfigInfoRetriever;
import ru.taskurotta.backend.console.retriever.DecisionInfoRetriever;
import ru.taskurotta.backend.console.retriever.GraphInfoRetriever;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.backend.console.retriever.ProfileInfoRetriever;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.console.retriever.TaskInfoRetriever;
import ru.taskurotta.backend.queue.TaskQueueItem;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * Default implementation of ConsoleManager
 * User: dimadin
 * Date: 17.05.13 16:39
 */
public class ConsoleManagerImpl implements ConsoleManager {

    private QueueInfoRetriever queueInfo;
    private ProcessInfoRetriever processInfo;
    private TaskInfoRetriever taskInfo;
    private CheckpointInfoRetriever checkpointInfo;
    private ProfileInfoRetriever profileInfo;
    private DecisionInfoRetriever decisionInfo;
    private ConfigInfoRetriever configInfo;
    private GraphInfoRetriever graphInfo;

    @Override
    public GenericPage<QueueVO> getQueuesState(int pageNumber, int pageSize) {
        if (queueInfo == null) {
            return null;
        }
        List<QueueVO> tmpResult = null;
        GenericPage<String> queuesPage = queueInfo.getQueueList(pageNumber, pageSize);
        if (queuesPage != null && queuesPage.getItems() != null) {
            tmpResult = new ArrayList<>();
            for (String queueName : queuesPage.getItems()) {
                QueueVO queueVO = new QueueVO();
                queueVO.setName(queueName);
                queueVO.setCount(queueInfo.getQueueTaskCount(queueName));
                tmpResult.add(queueVO);
            }
        }
        return new GenericPage<>(tmpResult, queuesPage.getPageNumber(), queuesPage.getPageSize(), queuesPage.getTotalCount());
    }

    @Override
    public Collection<TaskContainer> getProcessTasks(UUID processId) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.getProcessTasks(graphInfo.getProcessTasks(processId), processId);
    }

    @Override
    public GenericPage<TaskQueueItem> getEnqueueTasks(String queueName, int pageNum, int pageSize) {
        if (queueInfo == null) {
            return null;
        }
        return queueInfo.getQueueContent(queueName, pageNum, pageSize);
    }

    @Override
    public TaskContainer getTask(UUID taskId, UUID processId) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.getTask(taskId, processId);
    }

    @Override
    public DecisionContainer getDecision(UUID taskId, UUID processId) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.getDecision(taskId, processId);
    }

    @Override
    public ProcessVO getProcess(UUID processUuid) {
        if (processInfo == null) {
            return null;
        }
        return processInfo.getProcess(processUuid);
    }

    @Override
    public List<ProfileVO> getProfilesInfo() {
        if (profileInfo == null) {
            return null;
        }
        return profileInfo.getProfileInfo();
    }

    @Override
    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.listTasks(pageNumber, pageSize);
    }

    @Override
    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize) {
        if (processInfo == null) {
            return null;
        }
        return processInfo.listProcesses(pageNumber, pageSize);
    }

    @Override
    public TaskTreeVO getTreeForTask(UUID taskId, UUID processId) {
        TaskTreeVO result = new TaskTreeVO(taskId);
        TaskContainer task = taskInfo.getTask(taskId, processId);
        if (task != null) {
            result.setDesc(task.getActorId() + " - " + task.getMethod());
        }
        DecisionContainer decision = taskInfo.getDecision(taskId, processId);
        if (decision != null && decision.getTasks() != null && decision.getTasks().length != 0) {
            TaskTreeVO[] childs = new TaskTreeVO[decision.getTasks().length];
            for (int i = 0; i < decision.getTasks().length; i++) {
                TaskContainer childTask = decision.getTasks()[i];
                TaskTreeVO childTree = getTreeForTask(childTask.getTaskId(), processId);
                childTree.setParent(taskId);
                childTree.setDesc(childTask.getActorId() + " - " + childTask.getMethod());
                childs[i] = childTree;
            }
            result.setChildren(childs);
        }

        return result;
    }

    @Override
    public TaskTreeVO getTreeForProcess(UUID processUuid) {
        TaskTreeVO result = null;
        ProcessVO process = processInfo.getProcess(processUuid);
        if (process != null && process.getStartTaskUuid() != null) {
            result = getTreeForTask(process.getStartTaskUuid(), processUuid);
        }
        return result;
    }

    @Override
    public List<ProcessVO> findProcesses(String type, String id) {
        if (processInfo == null) {
            return null;
        }
        return processInfo.findProcesses(type, id);
    }

    @Override
    public List<QueueVO> getQueuesHovering(float periodSize) {
        if (queueInfo == null) {
            return null;
        }
        List<QueueVO> tmpResult = null;
        Map<String, Integer> queues = queueInfo.getHoveringCount(periodSize);
        if (queues != null) {
            tmpResult = new ArrayList<>();
            for (String queueName : queues.keySet()) {
                QueueVO queueVO = new QueueVO();
                queueVO.setName(queueName);
                queueVO.setCount(queues.get(queueName));
                tmpResult.add(queueVO);
            }
        }
        return tmpResult;
    }


    @Override
    public Collection<TaskContainer> getRepeatedTasks(int iterationCount) {
        return taskInfo.getRepeatedTasks(iterationCount);
    }

    @Override
    public void blockActor(String actorId) {
        configInfo.blockActor(actorId);
    }

    public void unblockActor(String actorId) {
        configInfo.unblockActor(actorId);
    }

    public void setQueueInfo(QueueInfoRetriever queueInfo) {
        this.queueInfo = queueInfo;
    }

    public void setProcessInfo(ProcessInfoRetriever processInfo) {
        this.processInfo = processInfo;
    }

    public void setTaskInfo(TaskInfoRetriever taskInfo) {
        this.taskInfo = taskInfo;
    }

    public void setCheckpointInfo(CheckpointInfoRetriever checkpointInfo) {
        this.checkpointInfo = checkpointInfo;
    }

    public void setProfileInfo(ProfileInfoRetriever profileInfo) {
        this.profileInfo = profileInfo;
    }

    public void setDecisionInfo(DecisionInfoRetriever decisionInfo) {
        this.decisionInfo = decisionInfo;
    }

    public void setConfigInfo(ConfigInfoRetriever configInfo) {
        this.configInfo = configInfo;
    }

    public void setGraphInfo(GraphInfoRetriever graphInfo) {
        this.graphInfo = graphInfo;
    }
}