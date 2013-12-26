package ru.taskurotta.server;

import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.queue.TaskQueueItem;
import ru.taskurotta.service.statistics.MetricFactory;
import ru.taskurotta.service.statistics.MetricName;
import ru.taskurotta.service.statistics.metrics.Metric;

import java.util.UUID;

/**
 * Wrapper for QueueService with data collection on "enqueue" metric
 * Date: 26.12.13 12:10
 */
public class MetricsQueueService implements QueueService {

    private QueueService queueService;
    private MetricFactory metricsFactory;

    public MetricsQueueService(QueueService queueService, MetricFactory metricsFactory) {
        this.queueService = queueService;
        this.metricsFactory = metricsFactory;
    }

    @Override
    public TaskQueueItem poll(String actorId, String taskList) {
        return queueService.poll(actorId, taskList);
    }

    @Override
    public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {
        boolean result = false;
        Metric enqueueMetric = metricsFactory.getInstance(MetricName.ENQUEUE.getValue());
        long metricStartTime = System.currentTimeMillis();
        result = queueService.enqueueItem(actorId, taskId, processId, startTime, taskList);
        long invocationTime = System.currentTimeMillis() - metricStartTime;
        enqueueMetric.mark(actorId, invocationTime);
        enqueueMetric.mark(MetricName.ENQUEUE.getValue(), invocationTime);
        return result;
    }

    @Override
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        return queueService.isTaskInQueue(actorId, taskList, taskId, processId);
    }

    @Override
    public String createQueueName(String actorId, String taskList) {
        return queueService.createQueueName(actorId, taskList);
    }

    @Override
    public long getLastPolledTaskEnqueueTime(String queueName) {
        return queueService.getLastPolledTaskEnqueueTime(queueName);
    }
}
