package ru.taskurotta.service.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.MetricsDataUtils;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.handler.NumberDataHandler;
import ru.taskurotta.service.metrics.model.DataPointVO;
import ru.taskurotta.transport.utils.TransportUtils;
import ru.taskurotta.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
public class MemoryQueueService implements QueueService, QueueInfoRetriever {

    private final static Logger logger = LoggerFactory.getLogger(MemoryQueueService.class);

    protected final ConcurrentHashMap<String, Long> lastPolledTaskEnqueueTimes = new ConcurrentHashMap<>();
    private long pollDelay = 60000l;
    private final Map<String, DelayQueue<DelayedTaskElement>> queues = new ConcurrentHashMap<>();

    public MemoryQueueService(long pollDelay) {

        this.pollDelay = pollDelay;

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
    public long getLastPolledTaskEnqueueTime(String queueName) {
        Long time = lastPolledTaskEnqueueTimes.get(queueName);

        // if no tasks in queue, than return -1
        if (time == null) {
            return -1;
        }

        return time;
    }

    @Override
    public void clearQueue(String queueName) {
        DelayQueue<DelayedTaskElement> queue = getQueue(queueName);
        queue.clear();
    }

    @Override
    public void removeQueue(String queueName) {
        DelayQueue<DelayedTaskElement> queue = queues.get(queueName);
        if (queue != null) {
            synchronized (queues) {
                queues.remove(queueName);
            }
        }
    }

    @Override
    public long getQueueDelaySize(String queueName) {
        return 0;//no storages for memory impl
    }

    @Override
    public Map<Date, String> getNotPollingQueues(long pollTimeout) {
        MetricsDataHandler metricsDataHandler = MetricsDataHandler.getInstance();
        Map<Date, String> result = new TreeMap<>(new Comparator<Date>() {
            @Override
            public int compare(Date date1, Date date2) {
                return date2.compareTo(date1);
            }
        });

        Collection<String> queueNames = getQueueNames();
        long now = System.currentTimeMillis();
        for (String queueName : queueNames) {
            Date lastActivity = metricsDataHandler.getLastActivityTime(MetricName.POLL.getValue(), queueName);
            if (lastActivity == null || (now - lastActivity.getTime()) > pollTimeout) {
                result.put(lastActivity == null ? new Date(0) : lastActivity, queueName);
            }
        }

        return result;

    }

    private List<String> getTaskQueueNames(String filter) {
        List<String> result = new ArrayList<>();
        for (String name: queues.keySet()) {
            if (StringUtils.isBlank(filter)
                    || name.startsWith(filter)) {
                result.add(name);
            }
        }
        return result;
    }


    @Override
    public int getQueueSize(String queueName) {
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

        String queueName = createQueueName(actorId, taskList);
        DelayQueue <DelayedTaskElement> queue = getQueue(queueName);

        TaskQueueItem result;

        try {
            result = queue.poll(pollDelay, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Error at polling task for actor["+actorId+"], taskList["+taskList+"]", e);
            throw new ServiceCriticalException("Error at polling task for actor["+actorId+"], taskList["+taskList+"]", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Poll for actorId [{}], taskList [{}] returned item [{}]. Remaining queue.size: [{}]", actorId, taskList, result, queue.size());
        }

        lastPolledTaskEnqueueTimes.put(queueName, result != null ? result.getEnqueueTime() : System.currentTimeMillis());

        return result;

    }

    @Override
    public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {

        // set it to current time for precisely repeat
        if (startTime <= 0L) {
            startTime = System.currentTimeMillis();
        }

        DelayQueue<DelayedTaskElement> queue = getQueue(createQueueName(actorId, taskList));
        boolean result = queue.add(new DelayedTaskElement(taskId, processId, startTime, System.currentTimeMillis()));

        if (logger.isDebugEnabled()) {
            logger.debug("EnqueueItem() for actorId [{}], taskList [{}], taskId [{}], startTime [{}]; Queue.size: [{}]", actorId, taskList, taskId, startTime, queue.size());
        }

        return result;
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
        return TransportUtils.createQueueName(actorId, taskList);
        //return (taskList == null) ? actorId : actorId + "#" + taskList;
    }

    @Override
    public GenericPage<QueueStatVO> getQueuesStatsPage(int pageNum, int pageSize, String filter) {
        GenericPage<QueueStatVO> result = null;
        List<String> allQueues = getTaskQueueNames(filter);
        if (allQueues!=null && !allQueues.isEmpty()) {
            int pageStart = (pageNum - 1) * pageSize;
            int pageEnd = Math.min(pageSize * pageNum, allQueues.size());

            List<String> queueNamesPage = allQueues.subList(pageStart, pageEnd);
            if (queueNamesPage!=null && !queueNamesPage.isEmpty()) {
                MetricsDataHandler mdh = MetricsDataHandler.getInstance();
                NumberDataHandler ndh = NumberDataHandler.getInstance();
                if (mdh != null && ndh != null) {
                    List<QueueStatVO> resultItems = new ArrayList<>();
                    for (String queueName: queueNamesPage) {
                        QueueStatVO item = new QueueStatVO();
                        item.setName(queueName);
                        Number count = ndh.getLastValue(MetricName.QUEUE_SIZE.getValue(), queueName);
                        item.setCount(count!=null? (Integer)count: 0);
                        item.setLastActivity(mdh.getLastActivityTime(MetricName.POLL.getValue(), queueName));

                        DataPointVO<Long>[] outHour = mdh.getCountsForLastHour(MetricName.SUCCESSFUL_POLL.getValue(), queueName);
                        DataPointVO<Long>[] outDay = mdh.getCountsForLastDay(MetricName.SUCCESSFUL_POLL.getValue(), queueName);

                        DataPointVO<Long>[] inHour = mdh.getCountsForLastHour(MetricName.ENQUEUE.getValue(), queueName);
                        DataPointVO<Long>[] inDay = mdh.getCountsForLastDay(MetricName.ENQUEUE.getValue(), queueName);

                        item.setInDay(MetricsDataUtils.sumUpDataPointsArray(inDay));
                        item.setInHour(MetricsDataUtils.sumUpDataPointsArray(inHour));

                        item.setOutDay(MetricsDataUtils.sumUpDataPointsArray(outDay));
                        item.setOutHour(MetricsDataUtils.sumUpDataPointsArray(outHour));

                        resultItems.add(item);
                    }

                    if (resultItems!=null && !resultItems.isEmpty()) {
                        result = new GenericPage<>(resultItems, pageNum, pageSize, allQueues.size());
                    }

                } else {
                    logger.error("Cannot extract dataHandlers, methodDataHandler["+mdh+"], numberDataHandler["+ndh+"]");
                }

            }

        }

        logger.debug("Result list of QueueStatVO is [{}]", result);
        return result;
    }

    @Override
    public Collection<String> getQueueNames() {
        return new ArrayList<>(queues.keySet());
    }

    /**
     * Drps and recreate queeu map
     */
    public void simulateDataLoss() {
        queues.clear();
    }
}
