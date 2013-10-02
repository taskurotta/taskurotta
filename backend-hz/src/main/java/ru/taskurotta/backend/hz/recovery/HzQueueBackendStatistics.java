package ru.taskurotta.backend.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.hz.queue.HzQueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;
import ru.taskurotta.backend.recovery.AbstractQueueBackendStatistics;
import ru.taskurotta.backend.statistics.MetricFactory;
import ru.taskurotta.backend.statistics.metrics.Metric;
import ru.taskurotta.server.MetricName;

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
public class HzQueueBackendStatistics extends AbstractQueueBackendStatistics {

    private HzQueueBackend queueBackend;
    private HazelcastInstance hzInstance;

    private MetricFactory metricsFactory;

    public static final String lastPolledTaskEnqueueTimesName = "lastPolledTaskEnqueueTimes";

    class StatisticsMerger implements Runnable {
        @Override
        public void run() {
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
        }
    }

    public HzQueueBackendStatistics(HzQueueBackend queueBackend, HazelcastInstance hzInstance, long mergePeriod, TimeUnit mergePeriodTimeUnit) {
        super(queueBackend);

        this.queueBackend = queueBackend;
        this.hzInstance = hzInstance;

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new StatisticsMerger(), 0, mergePeriod, mergePeriodTimeUnit);
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
    public void enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {
        long start = System.currentTimeMillis();
        queueBackend.enqueueItem(actorId, taskId, processId, startTime, taskList);
        long invocationTime = System.currentTimeMillis() - start;

        Metric enqueueMetric = metricsFactory.getInstance(MetricName.ENQUEUE.getValue());
        enqueueMetric.mark(MetricName.ENQUEUE.getValue(), invocationTime);
        enqueueMetric.mark(actorId, invocationTime);
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        return queueBackend.getQueueContent(queueName, pageNum, pageSize);
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return queueBackend.getHoveringCount(periodSize);
    }

    public void setMetricsFactory(MetricFactory metricsFactory) {
        this.metricsFactory = metricsFactory;
    }
}
