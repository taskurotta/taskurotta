package ru.taskurotta.console.manager.impl;

import ru.taskurotta.console.manager.ConsoleManager;
import ru.taskurotta.console.model.QueueVO;
import ru.taskurotta.console.retriever.CheckpointInfoRetriever;
import ru.taskurotta.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.console.retriever.QueueInfoRetriever;
import ru.taskurotta.console.retriever.TaskInfoRetriever;

import java.util.ArrayList;
import java.util.List;

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
            result = new ArrayList<QueueVO>();
            for(String queueName: queues) {
                QueueVO queueVO = new QueueVO();
                queueVO.setName(queueName);
                queueVO.setCount(queueInfo.getQueueTaskCount(queueName));
                result.add(queueVO);
            }
        }
        return result;
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
