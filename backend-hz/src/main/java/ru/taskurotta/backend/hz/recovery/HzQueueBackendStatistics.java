package ru.taskurotta.backend.hz.recovery;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.hz.queue.HzQueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;
import ru.taskurotta.backend.recovery.AbstractQueueBackendStatistics;
import ru.taskurotta.backend.statistics.MetricFactory;
import ru.taskurotta.backend.statistics.datalistener.NumberDataListener;
import ru.taskurotta.backend.statistics.metrics.Metric;
import ru.taskurotta.backend.statistics.metrics.TimeConstants;
import ru.taskurotta.server.MetricName;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 18.09.13 Time: 11:50
 */
public class HzQueueBackendStatistics extends AbstractQueueBackendStatistics implements TimeConstants {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueBackendStatistics.class);
    private HzQueueBackend queueBackend;
    private HazelcastInstance hzInstance;

    private long mergePeriod;
    private TimeUnit mergePeriodTimeUnit;
    private String queueNamePrefix;

    private long queueSizeMetricPeriodSeconds;

    private MetricFactory metricsFactory;
    private NumberDataListener numberDataListener;

    public static final String lastPolledTaskEnqueueTimesName = "lastPolledTaskEnqueueTimes";

    class StatisticsMerger implements Runnable {
        @Override
        public void run() {
            try { //should always wrap ScheduledExecutorService tasks to try-catch to prevent silent task death
                IMap<String, Long> iMap = hzInstance.getMap(lastPolledTaskEnqueueTimesName);

                // merge from local map to distributed map
                Set<Map.Entry<String, Long>> entries = lastPolledTaskEnqueueTimes.entrySet();
                for (Map.Entry<String, Long> entry : entries) {
                    String queueName = entry.getKey();
                    Long lastEnqueueTime = entry.getValue();

                    Long previousEnqueueTime = iMap.get(queueName);
                    if (previousEnqueueTime == null) {
                        iMap.put(queueName, lastEnqueueTime);
                    } else {
                        if (previousEnqueueTime < lastEnqueueTime) {
                            iMap.put(queueName, lastEnqueueTime);
                        }
                    }
                }

                // merge from distributed map to local
                entries = iMap.entrySet();
                for (Map.Entry<String, Long> entry : entries) {
                    String queueName = entry.getKey();
                    Long lastEnqueueTime = entry.getValue();

                    Long previousEnqueueTime = lastPolledTaskEnqueueTimes.get(queueName);
                    if (previousEnqueueTime == null) {
                        lastPolledTaskEnqueueTimes.put(queueName, lastEnqueueTime);
                    } else {
                        if (previousEnqueueTime < lastEnqueueTime) {
                            lastPolledTaskEnqueueTimes.put(queueName, lastEnqueueTime);
                        }
                    }
                }

            } catch (Throwable e) {
                logger.error("StatisticsMerger iteration failed", e);
            }
        }
    }

    class QueueSizeDataFlusher implements Runnable {

        private int dataSize = 0;
        QueueSizeDataFlusher (int dataSize) {
            this.dataSize = dataSize;
        }

        @Override
        public void run() {
            try {
                int count = 0;
                int totalSize = 0;
                if (numberDataListener != null) {
                    for (String queue : getQueueNames()) {
                        int queueSize = getQueueTaskCount(queue);
                        numberDataListener.handleNumberData(MetricName.QUEUE_SIZE.getValue(), queue, queueSize, System.currentTimeMillis(), dataSize);
                        count++;
                        totalSize+=queueSize;
                    }
                    numberDataListener.handleNumberData(MetricName.QUEUE_SIZE.getValue(), MetricName.QUEUE_SIZE.getValue(), totalSize, System.currentTimeMillis(), dataSize);
                }
                logger.debug("Queue size data items [{}] successfully flushed", count);

            } catch (Throwable e) {
                logger.error("QueueDataFlusher iteration failed", e);
            }
        }
    }

    private List<String> getQueueNames() {
        List<String> result = new ArrayList<>();
        for (DistributedObject inst : hzInstance.getDistributedObjects()) {
            if (inst instanceof IQueue) {
                String name = inst.getName();
                if (name.startsWith(queueNamePrefix)) {
                    result.add(name);
                }
            }
        }
        return result;
    }


    public HzQueueBackendStatistics(HzQueueBackend queueBackend, HazelcastInstance hzInstance) {
        super(queueBackend);

        this.queueBackend = queueBackend;
        this.hzInstance = hzInstance;

    }

    @PostConstruct
    public void init() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        //Queue statistics for recovery
        scheduledExecutorService.scheduleAtFixedRate(new StatisticsMerger(), 0, mergePeriod, mergePeriodTimeUnit);

        //Queue statistics for metrics
        int dataPointsCount = SECONDS_IN_24_HOURS/Long.valueOf(queueSizeMetricPeriodSeconds).intValue();//number of points to cover 24 hours period.
        scheduledExecutorService.scheduleAtFixedRate(new QueueSizeDataFlusher(dataPointsCount), 0, queueSizeMetricPeriodSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        return queueBackend.isTaskInQueue(actorId, taskList, taskId, processId);
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        return queueBackend.getQueueList(pageNum, pageSize);
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return queueBackend.getQueueTaskCount(queueName);
    }

    @Override
    public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {
        long start = System.currentTimeMillis();
        boolean result = queueBackend.enqueueItem(actorId, taskId, processId, startTime, taskList);
        long invocationTime = System.currentTimeMillis() - start;

        Metric enqueueMetric = metricsFactory.getInstance(MetricName.ENQUEUE.getValue());
        enqueueMetric.mark(MetricName.ENQUEUE.getValue(), invocationTime);
        enqueueMetric.mark(actorId, invocationTime);

        return result;
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        return queueBackend.getQueueContent(queueName, pageNum, pageSize);
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return queueBackend.getHoveringCount(periodSize);
    }

    @Required
    public void setMergePeriod(long mergePeriod) {
        this.mergePeriod = mergePeriod;
    }

    @Required
    public void setMergePeriodTimeUnit(TimeUnit mergePeriodTimeUnit) {
        this.mergePeriodTimeUnit = mergePeriodTimeUnit;
    }

    @Required
    public void setMetricsFactory(MetricFactory metricsFactory) {
        this.metricsFactory = metricsFactory;
    }

    @Required
    public void setQueueNamePrefix(String queueNamePrefix) {
        this.queueNamePrefix = queueNamePrefix;
    }

    @Required
    public void setQueueSizeMetricPeriodSeconds(long queueSizeMetricPeriodSeconds) {
        this.queueSizeMetricPeriodSeconds = queueSizeMetricPeriodSeconds;
    }

    @Required
    public void setNumberDataListener(NumberDataListener numberDataListener) {
        this.numberDataListener = numberDataListener;
    }
}
