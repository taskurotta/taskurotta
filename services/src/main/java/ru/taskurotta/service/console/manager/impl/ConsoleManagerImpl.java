package ru.taskurotta.service.console.manager.impl;

import ru.taskurotta.service.console.manager.ConsoleManager;
import ru.taskurotta.service.console.model.ActorState;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.console.model.TaskTreeVO;
import ru.taskurotta.service.console.retriever.ConfigInfoRetriever;
import ru.taskurotta.service.console.retriever.GraphInfoRetriever;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.console.retriever.TaskInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.service.queue.TaskQueueItem;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of ConsoleManager
 * User: dimadin
 * Date: 17.05.13 16:39
 */
public class ConsoleManagerImpl implements ConsoleManager {

    private QueueInfoRetriever queueInfo;
    private ProcessInfoRetriever processInfo;
    private TaskInfoRetriever taskInfo;
    private ConfigInfoRetriever configInfo;
    private GraphInfoRetriever graphInfo;

    private long pollTimeout;

    public ConsoleManagerImpl(QueueInfoRetriever queueInfo, ProcessInfoRetriever processInfo,
                              TaskInfoRetriever taskInfo, ConfigInfoRetriever configInfo,
                              GraphInfoRetriever graphInfo, long pollTimeout) {
        this.queueInfo = queueInfo;
        this.processInfo = processInfo;
        this.taskInfo = taskInfo;
        this.configInfo = configInfo;
        this.graphInfo = graphInfo;
        this.pollTimeout = pollTimeout;
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
        return taskInfo.getTaskWithLastDecision(taskId, processId);
    }

    @Override
    public DecisionContainer getDecision(UUID taskId, UUID processId) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.getDecisionContainer(taskId, processId);
    }

    @Override
    public Process getProcess(UUID processUuid) {
        if (processInfo == null) {
            return null;
        }
        return processInfo.getProcess(processUuid);
    }

    @Override
    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.listTasks(pageNumber, pageSize);
    }

    @Override
    public TaskTreeVO getTreeForTask(UUID taskId, UUID processId) {
        TaskTreeVO result = new TaskTreeVO(taskId);
        TaskContainer task = taskInfo.getTask(taskId, processId);
        if (task != null) {
            result.setDesc(task.getActorId() + " - " + task.getMethod());
        }
        // todo: get Decision and set state about "NotReady", "InQueue", errorAttempts and recoveryTime
        DecisionContainer decision = taskInfo.getDecisionContainer(taskId, processId);
        result.setState(getTaskTreeStatus(decision));

        if (decision != null) {
            TaskContainer[] taskDecisions = decision.getTasks();

            if (taskDecisions != null && taskDecisions.length != 0) {
                TaskTreeVO[] childs = new TaskTreeVO[taskDecisions.length];
                for (int i = 0; i < taskDecisions.length; i++) {
                    TaskContainer childTask = taskDecisions[i];
                    TaskTreeVO childTree = getTreeForTask(childTask.getTaskId(), processId);
                    childTree.setParent(taskId);
                    childTree.setDesc(childTask.getActorId() + " - " + childTask.getMethod());
                    childs[i] = childTree;
                }
                result.setChildren(childs);
            }
        }

        return result;
    }

    private int getTaskTreeStatus(DecisionContainer dc) {
        if (dc == null) {
            return TaskTreeVO.STATE_NOT_ANSWERED;
        } else if (dc.getErrorContainer() != null) {
            return TaskTreeVO.STATE_ERROR;
        } else {
            return TaskTreeVO.STATE_SUCCESS;
        }
    }

    @Override
    public TaskTreeVO getTreeForProcess(UUID processUuid) {
        TaskTreeVO result = null;
        Process process = processInfo.getProcess(processUuid);
        if (process != null && process.getStartTaskId() != null) {
            result = getTreeForTask(process.getStartTaskId(), processUuid);
        }
        return result;
    }

    @Override
    public GenericPage<Process> findProcesses(ProcessSearchCommand command) {
        if (processInfo == null) {
            return null;
        }
        return processInfo.findProcesses(command);
    }

    @Override
    public List<TaskContainer> findTasks(String processId, String taskId) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.findTasks(new TaskSearchCommand(processId, taskId));
    }

    @Override
    public Collection<TaskContainer> getRepeatedTasks(int iterationCount) {
        return taskInfo.getRepeatedTasks(iterationCount);
    }

    @Override
    public GenericPage<QueueStatVO> getQueuesStatInfo(int pageNumber, int pageSize, String filter) {

        GenericPage<QueueStatVO> model = queueInfo.getQueuesStatsPage(pageNumber, pageSize, filter);

        if (model != null) {
            List<QueueStatVO> list = model.getItems();
            if (list != null) {

                for (QueueStatVO queueVO : list) {
                    ActorState actorState = ActorState.ACTIVE;
                    String actorId = queueVO.getName();

                    if (configInfo.isActorBlocked(queueVO.getName())) {
                        actorState = ActorState.BLOCKED;
                    } else {
                        Date lastActivityTime = queueVO.getLastActivity();
                        if (lastActivityTime == null ||
                                isActorInactive(actorId, lastActivityTime.getTime())) {
                            actorState = ActorState.INACTIVE;
                        }
                    }

                    queueVO.setState(actorState);
                }
            }
        }

        return model;
    }

    private boolean isActorInactive(String actorId, long lastActivity) {
        if (System.currentTimeMillis() - lastActivity > pollTimeout) {
            return true;
        }

        return false;
    }

    @Override
    public int getFinishedCount(String customId) {
        return processInfo.getFinishedCount(customId);
    }

}
