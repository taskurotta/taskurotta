package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.hz.queue.HzQueueService;
import ru.taskurotta.service.queue.TaskQueueItem;
import ru.taskurotta.service.recovery.AbstractQueueServiceStatistics;
import ru.taskurotta.service.statistics.MetricFactory;
import ru.taskurotta.service.statistics.MetricName;
import ru.taskurotta.service.statistics.datalistener.NumberDataListener;
import ru.taskurotta.service.statistics.metrics.Metric;
import ru.taskurotta.service.statistics.metrics.TimeConstants;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
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
public class HzQueueServiceStatistics extends AbstractQueueServiceStatistics {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueServiceStatistics.class);

    private static final String SYNCH_LOCK_NAME = HzQueueServiceStatistics.class.getName().concat("#SINCH_LOCK");

    private HzQueueService queueService;
    private HazelcastInstance hzInstance;
    private ILock synchLock;

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

            synchLock.lock();

            try { //should always wrap ScheduledExecutorService tasks to try-catch to prevent silent task death

                IMap<String, Long> mutualMap = hzInstance.getMap(lastPolledTaskEnqueueTimesName);
                Map<String, Long> myMap = lastPolledTaskEnqueueTimes;

                Set<String> keys = new HashSet<>();
                keys.addAll(mutualMap.keySet());
                keys.addAll(myMap.keySet());

                for (String key: keys) {
                    Long mutualValue = mutualMap.get(key);
                    Long myValue = myMap.get(key);

                    if (mutualValue == null) {
                        mutualMap.set(key, myValue);
                        continue;
                    }

                    if (myValue == null) {
                        myMap.put(key, mutualValue);
                        continue;
                    }

                    if (myValue > mutualValue) {
                        mutualMap.set(key, myValue);
                    } else {
                        myMap.put(key, mutualValue);
                    }
                }

            } catch (Throwable e) {
                logger.error("StatisticsMerger iteration failed", e);
            } finally {
                synchLock.unlock();
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
                        numberDataListener.handleNumberData(MetricName.QUEUE_SIZE.getValue(), queue, queueSize,
                                System.currentTimeMillis(), dataSize);
                        count++;
                        totalSize+=queueSize;
                    }
                    numberDataListener.handleNumberData(MetricName.QUEUE_SIZE.getValue(),
                            MetricName.QUEUE_SIZE.getValue(), totalSize, System.currentTimeMillis(), dataSize);
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


    public HzQueueServiceStatistics(HzQueueService queueService, HazelcastInstance hzInstance) {
        super(queueService);

        this.queueService = queueService;
        this.hzInstance = hzInstance;

        this.synchLock = hzInstance.getLock(SYNCH_LOCK_NAME);
    }

    @PostConstruct
    public void init() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        //Queue statistics for recovery
        scheduledExecutorService.scheduleAtFixedRate(new StatisticsMerger(), 0, mergePeriod, mergePeriodTimeUnit);

        //Queue statistics for metrics
        int dataPointsCount = TimeConstants.SECONDS_IN_24_HOURS/Long.valueOf(queueSizeMetricPeriodSeconds).intValue()
                ;//number of points to cover 24 hours period.
        scheduledExecutorService.scheduleAtFixedRate(new QueueSizeDataFlusher(dataPointsCount), 0,
                queueSizeMetricPeriodSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        return queueService.isTaskInQueue(actorId, taskList, taskId, processId);
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        return queueService.getQueueList(pageNum, pageSize);
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return queueService.getQueueTaskCount(queueName);
    }

    @Override
    public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {
        long start = System.currentTimeMillis();
        boolean result = queueService.enqueueItem(actorId, taskId, processId, startTime, taskList);
        long invocationTime = System.currentTimeMillis() - start;

        Metric enqueueMetric = metricsFactory.getInstance(MetricName.ENQUEUE.getValue());
        enqueueMetric.mark(MetricName.ENQUEUE.getValue(), invocationTime);
        enqueueMetric.mark(actorId, invocationTime);

        return result;
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        return queueService.getQueueContent(queueName, pageNum, pageSize);
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return queueService.getHoveringCount(periodSize);
    }

    @Override
    public GenericPage<QueueStatVO> getQueuesStatsPage(int pageNum, int pageSize, String filter) {
        return queueService.getQueuesStatsPage(pageNum, pageSize, filter);
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
