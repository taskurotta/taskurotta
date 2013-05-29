package ru.taskurotta.backend.queue;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Profiled;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.QueuedTaskVO;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:11 PM
 */
public class MemoryQueueBackend implements QueueBackend, QueueInfoRetriever {

    private final static Logger logger = LoggerFactory.getLogger(MemoryQueueBackend.class);

    private int pollDelay = 60;
    private TimeUnit pollDelayUnit = TimeUnit.SECONDS;
    private final Map<String, DelayQueue<DelayedTaskElement>> queues = new ConcurrentHashMap<String, DelayQueue<DelayedTaskElement>>();
    private CheckpointService checkpointService = new MemoryCheckpointService();
    private Lock lock = new ReentrantLock();

    public MemoryQueueBackend(int pollDelay) {

        this.pollDelay = pollDelay;
    }

    public MemoryQueueBackend(int pollDelay, TimeUnit pollDelayUnit) {

        this.pollDelay = pollDelay;
        this.pollDelayUnit = pollDelayUnit;

        if (logger.isTraceEnabled()) {
            Thread monitor = new Thread() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(30000l);
                            StringBuilder sb = new StringBuilder();
                            for (String queue : queues.keySet()) {
                                sb.append(queue).append(": count ").append(getQueue(queue).size()).append("\n");
                            }
                            logger.trace("Queue monitor: \n {}", sb.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            monitor.setDaemon(true);
            monitor.start();
        }
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        List<String> result = new ArrayList<>();
        String[] queueNames = new String[queues.keySet().size()];
        queueNames = queues.keySet().toArray(queueNames);
        if (!queues.isEmpty()) {
            for (int i = (pageNum - 1) * pageSize; i <= ((pageSize * pageNum >= (queueNames.length)) ? (queueNames.length) - 1 : pageSize * pageNum - 1); i++) {
                result.add(queueNames[i]);
            }
        }
        return new GenericPage<String>(result, pageNum, pageSize, queues.size());
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return getQueue(queueName).size();
    }

    @Override
    public GenericPage<QueuedTaskVO> getQueueContent(String queueName, int pageNum, int pageSize) {
        List<QueuedTaskVO> result = new ArrayList<QueuedTaskVO>();
        DelayedTaskElement[] tasks = new DelayedTaskElement[getQueue(queueName).size()];
        tasks = getQueue(queueName).toArray(tasks);

        if (tasks.length > 0) {
            for (int i = (pageNum - 1) * pageSize; i <= ((pageSize * pageNum >= (tasks.length)) ? (tasks.length) - 1 : pageSize * pageNum - 1); i++) {
                DelayedTaskElement dte = tasks[i];
                QueuedTaskVO qt = new QueuedTaskVO();
                qt.setId(dte.taskId);
                qt.setInsertTime(dte.enqueueTime);
                qt.setStartTime(dte.startTime);
                result.add(qt);
            }
        }
        return new GenericPage<QueuedTaskVO>(result, pageNum, pageSize, tasks.length);
    }


    /**
     * Helper class for Delayed queue
     */
    private static class DelayedTaskElement implements Delayed {

        protected UUID taskId;

        protected long startTime;

        protected long enqueueTime;

        public DelayedTaskElement(UUID taskId, long startTime, long enqueueTime) {

            this.taskId = taskId;
            this.startTime = startTime;
            this.enqueueTime = enqueueTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(startTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.valueOf(((DelayedTaskElement) o).startTime).compareTo(startTime);
        }

        /**
         * startTime not used because we assume than no duplication in queue
         *
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DelayedTaskElement)) return false;

            DelayedTaskElement that = (DelayedTaskElement) o;

            if (!taskId.equals(that.taskId)) return false;

            return true;
        }

        /**
         * startTime not used because we assume than no duplication in queue
         */
        @Override
        public int hashCode() {
            return taskId.hashCode();
        }
    }

    @Override
    @Profiled(notNull = true)
    public UUID poll(String actorId, String taskList) {
        DelayQueue<DelayedTaskElement> queue = getQueue(createQueueName(actorId, taskList));

        UUID taskId = null;
        try {

            DelayedTaskElement delayedTaskObject = queue.poll(pollDelay, pollDelayUnit);

            if (delayedTaskObject != null) {
                taskId = delayedTaskObject.taskId;
                checkpointService.addCheckpoint(new Checkpoint(TimeoutType.TASK_POLL_TO_COMMIT, taskId, actorId, System.currentTimeMillis()));
            }

        } catch (InterruptedException e) {
            e.printStackTrace();

            // TODO: Where general policy about exceptions ?
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        logger.debug("poll() returns taskId [{}]. Queue.size: {}", taskId, queue.size());

        return taskId;

    }

    @Override
    @Profiled
    public void pollCommit(String actorId, UUID taskId) {
        checkpointService.removeEntityCheckpoints(taskId, TimeoutType.TASK_SCHEDULE_TO_START);
        checkpointService.removeEntityCheckpoints(taskId, TimeoutType.TASK_POLL_TO_COMMIT);
    }

    @Override
    @Profiled
    public void enqueueItem(String actorId, UUID taskId, long startTime, String taskList) {

        DelayQueue<DelayedTaskElement> queue = getQueue(createQueueName(actorId, taskList));
        queue.add(new DelayedTaskElement(taskId, startTime, System.currentTimeMillis()));

        //Checkpoints for SCHEDULED_TO_START, SCHEDULE_TO_CLOSE timeouts
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.TASK_SCHEDULE_TO_START, taskId, actorId, startTime));
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.TASK_SCHEDULE_TO_CLOSE, taskId, actorId, startTime));
        logger.debug("enqueueItem() actorId [{}], taskId [{}], startTime [{}]; Queue.size: {}", actorId, taskId, startTime, queue.size());
    }


    private DelayQueue<DelayedTaskElement> getQueue(String queueName) {

        DelayQueue<DelayedTaskElement> queue = queues.get(queueName);
        if (queue == null) {
            synchronized (queues) {

                queue = queues.get(queueName);
                if (queue == null) {
                    queue = new DelayQueue<DelayedTaskElement>();
                    queues.put(queueName, queue);
                }
            }
        }
        return queue;
    }

    public boolean isTaskInQueue(ActorDefinition actorDefinition, UUID taskId) {
        DelayQueue<DelayedTaskElement> queue = getQueue(createQueueName(actorDefinition.getFullName(), actorDefinition.getTaskList()));

        DelayedTaskElement delayedTaskElement = new DelayedTaskElement(taskId, 0, System.currentTimeMillis());

        return queue.contains(delayedTaskElement);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    private String createQueueName(String actorId, String taskList) {
        return (taskList == null) ? actorId : actorId + "#" + taskList;
    }
}
