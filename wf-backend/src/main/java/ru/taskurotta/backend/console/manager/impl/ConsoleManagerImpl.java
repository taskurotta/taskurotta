package ru.taskurotta.backend.console.manager.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.console.manager.ConsoleManager;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.model.QueueVO;
import ru.taskurotta.backend.console.model.QueuedTaskVO;
import ru.taskurotta.backend.console.retriever.CheckpointInfoRetriever;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.console.retriever.TaskInfoRetriever;
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


    @Override
    public GenericPage<QueueVO> getQueuesState(int pageNumber, int pageSize) {
        if (queueInfo == null) {
            return null;
        }
        List<QueueVO> tmpResult = null;
        GenericPage<String> queuesPage = queueInfo.getQueueList(pageNumber, pageSize);
        if (queuesPage != null) {
            tmpResult = new ArrayList<>();
            for (String queueName : queuesPage.getItems()) {
                QueueVO queueVO = new QueueVO();
                queueVO.setName(queueName);
                queueVO.setCount(queueInfo.getQueueTaskCount(queueName));
                tmpResult.add(queueVO);
            }
        }
        return new GenericPage<QueueVO>(tmpResult, queuesPage.getPageNumber(), queuesPage.getPageSize(), queuesPage.getTotalCount());
    }

    @Override
    public List<TaskContainer> getProcessTasks(UUID processUuid) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.getProcessTasks(processUuid);
    }

    @Override
    public List<QueuedTaskVO> getEnqueueTasks(String queueName) {
        if (queueInfo == null) {
            return null;
        }
        return queueInfo.getQueueContent(queueName);
    }

    @Override
    public TaskContainer getTask(UUID taskId) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.getTask(taskId);
    }

    @Override
    public ProcessVO getProcess(UUID processUuid) {
        if (processInfo == null) {
            return null;
        }
        return processInfo.getProcess(processUuid);
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
}
