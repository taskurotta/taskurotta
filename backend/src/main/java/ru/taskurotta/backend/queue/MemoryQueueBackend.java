package ru.taskurotta.backend.queue;

import net.sf.cglib.core.CollectionUtils;
import net.sf.cglib.core.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.exception.BackendCriticalException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:11 PM
 */
public class MemoryQueueBackend implements QueueBackend, QueueInfoRetriever {

    private final static Logger logger = LoggerFactory.getLogger(MemoryQueueBackend.class);

    private int pollDelay = 60;
    private TimeUnit pollDelayUnit = TimeUnit.SECONDS;
    private final Map<String, DelayQueue<DelayedTaskElement>> queues = new ConcurrentHashMap<>();

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
        return new GenericPage<>(result, pageNum, pageSize, queues.size());
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return getQueue(queueName).size();
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        List<TaskQueueItem> result = new ArrayList<>();
        DelayedTaskElement[] tasks = new DelayedTaskElement[getQueue(queueName).size()];
        tasks = getQueue(queueName).toArray(tasks);

        if (tasks.length > 0) {
            for (int i = (pageNum - 1) * pageSize; i <= ((pageSize * pageNum >= (tasks.length)) ? (tasks.length) - 1 : pageSize * pageNum - 1); i++) {
                DelayedTaskElement dte = tasks[i];
                result.add(dte);
            }
        }
        return new GenericPage<>(result, pageNum, pageSize, tasks.length);
    }


    /**
     * Helper class for Delayed queue
     */
    private static class DelayedTaskElement extends TaskQueueItem implements Delayed {

        public DelayedTaskElement(UUID taskId, UUID processId, long startTime, long enqueueTime) {
            setTaskId(taskId);
            setProcessId(processId);
            setStartTime(startTime);
            setEnqueueTime(enqueueTime);
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
            if (!processId.equals(that.processId)) return false;

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
    public TaskQueueItem poll(String actorId, String taskList) {
        DelayQueue<DelayedTaskElement> queue = getQueue(createQueueName(actorId, taskList));

        TaskQueueItem result;

        try {
            result = queue.poll(pollDelay, pollDelayUnit);
        } catch (InterruptedException e) {
            logger.error("Error at polling task for actor["+actorId+"], taskList["+taskList+"]", e);
            throw new BackendCriticalException("Error at polling task for actor["+actorId+"], taskList["+taskList+"]", e);
        }

        logger.debug("Task poll for actorId[{}], taskList[{}] returned item [{}]. Remaining queue.size: [{}]", actorId, taskList, result, queue.size());

        return result;

    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }

        DelayQueue<DelayedTaskElement> queue = getQueue(createQueueName(actorId, taskList));
        queue.add(new DelayedTaskElement(taskId, processId, startTime, System.currentTimeMillis()));

        logger.debug("enqueueItem() actorId [{}], taskId [{}], startTime [{}]; Queue.size: {}", actorId, taskId, startTime, queue.size());
    }


    private DelayQueue<DelayedTaskElement> getQueue(String queueName) {

        DelayQueue<DelayedTaskElement> queue = queues.get(queueName);
        if (queue == null) {
            synchronized (queues) {

                queue = queues.get(queueName);
                if (queue == null) {
                    queue = new DelayQueue<>();
                    queues.put(queueName, queue);
                }
            }
        }
        return queue;
    }

    @Override
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        DelayQueue<DelayedTaskElement> queue = getQueue(createQueueName(actorId, taskList));

        DelayedTaskElement delayedTaskElement = new DelayedTaskElement(taskId, processId, 0, System.currentTimeMillis());

        return queue.contains(delayedTaskElement);
    }

    @Override
    public String createQueueName(String actorId, String taskList) {
        return (taskList == null) ? actorId : actorId + "#" + taskList;
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        Map<String, Integer> result = new HashMap<>();
        String[] queueNames = new String[queues.keySet().size()];
        int days = (int) (periodSize / 1);
        int hours = (int) ((periodSize - days) * 24);
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.DATE, -days);
        endDate.add(Calendar.HOUR, -hours);
        final Date tmpDate = endDate.getTime();
        if (!queues.isEmpty()) {
            for (String queueName : queueNames) {
                DelayQueue<DelayedTaskElement> queue = queues.get(queueName);
                int count = CollectionUtils.filter(queue, new Predicate() {
                    @Override
                    public boolean evaluate(Object o) {
                        DelayedTaskElement task = (DelayedTaskElement) o;
                        return task.startTime < tmpDate.getTime();
                    }
                }).size();
                result.put(queueName, count);
            }
        }
        return result;
    }
}
