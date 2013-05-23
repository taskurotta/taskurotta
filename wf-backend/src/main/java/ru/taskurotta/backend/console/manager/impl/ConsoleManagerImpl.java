package ru.taskurotta.backend.console.manager.impl;

import ru.taskurotta.backend.console.manager.ConsoleManager;
import ru.taskurotta.backend.console.model.QueueVO;
import ru.taskurotta.backend.console.model.QueuedTaskVO;
import ru.taskurotta.backend.console.retriever.CheckpointInfoRetriever;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.console.retriever.TaskInfoRetriever;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
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
    private CheckpointInfoRetriever checkpointInfo;

    @Override
    public List<QueueVO> getQueuesState() {
        List<QueueVO> result = null;
        List<String> queues = queueInfo.getQueueList();
        if(queues!=null && !queues.isEmpty()) {
            result = new ArrayList<>();
            for(String queueName: queues) {
                QueueVO queueVO = new QueueVO();
                queueVO.setName(queueName);
                queueVO.setCount(queueInfo.getQueueTaskCount(queueName));
                result.add(queueVO);
            }
        }
        return result;
    }

    @Override
    public List<TaskContainer> getProcessTasks(UUID processUuid) {
        return taskInfo.getProcessTasks(processUuid);
    }

    @Override
    public List<QueuedTaskVO> getEnqueueTasks(String queueName) {
        return queueInfo.getQueueContent(queueName);
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
