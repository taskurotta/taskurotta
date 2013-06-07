package ru.taskurotta.backend.hz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.QueuedTaskVO;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.util.ActorDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by void 07.06.13 11:00
 */
public class HazelcastQueueBackend implements QueueBackend, QueueInfoRetriever {

    private final static Logger logger = LoggerFactory.getLogger(HazelcastQueueBackend.class);


    @Override
    public UUID poll(String actorId, String taskList) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void pollCommit(String actorId, UUID taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, long startTime, String taskList) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CheckpointService getCheckpointService() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public GenericPage<QueuedTaskVO> getQueueContent(String queueName, int pageNum, int pageSize) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
