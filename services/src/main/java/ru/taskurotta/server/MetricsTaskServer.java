package ru.taskurotta.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.metrics.Metric;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.MetricsFactory;
import ru.taskurotta.service.metrics.PeriodicMetric;
import ru.taskurotta.service.metrics.PeriodicMetric.DatasetValueExtractor;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * TaskServer wrapper delegating method calls to enclosed server with metrics data collect operations
 * Date: 27.08.13 14:39
 */
public class MetricsTaskServer implements TaskServer {

    private TaskServer taskServer;
    private MetricsFactory metricsFactory;

    private static final Logger logger = LoggerFactory.getLogger(MetricsTaskServer.class);

    public MetricsTaskServer(TaskServer taskServer, MetricsFactory metricsFactory, int memoryMetricsPeriodSeconds) {
        this.taskServer = taskServer;
        this.metricsFactory = metricsFactory;

        PeriodicMetric memoryMetric = metricsFactory.getPeriodicInstance(MetricName.MEMORY.getValue(), memoryMetricsPeriodSeconds);
        memoryMetric.periodicMark(new DatasetValueExtractor() {

            private static final String FREE_MEM = "free";
            private static final String TOTAL_MEM = "total";
            private Runtime runtime = Runtime.getRuntime();

            @Override
            public List<String> getDatasets() {
                return Arrays.asList(FREE_MEM, TOTAL_MEM);
            }

            @Override
            public Number getDatasetValue(String dataset) {
                if (TOTAL_MEM.equals(dataset)) {
                    return runtime.totalMemory();
                } else if (FREE_MEM.equals(dataset)) {
                    return runtime.freeMemory();
                } else {
                    return 0;
                }
            }

            @Override
            public Number getGeneralValue(Map<String, Number> datasetsValues) {
                Number result = 0l;
                if (datasetsValues!=null && !datasetsValues.isEmpty()) {
                    result = (long)datasetsValues.get(TOTAL_MEM) - (long)datasetsValues.get(FREE_MEM);
                }
                return result;
            }
        });
    }


    @Override
    public void startProcess(TaskContainer task) {

        String actorId = task.getActorId();

        long startTime = System.currentTimeMillis();

        taskServer.startProcess(task);

        long invocationTime = System.currentTimeMillis()-startTime;
        Metric startProcessMetric = metricsFactory.getInstance(MetricName.START_PROCESS.getValue());
        startProcessMetric.mark(actorId, invocationTime);
        startProcessMetric.mark(MetricName.START_PROCESS.getValue(), invocationTime);

    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        String actorId = ActorUtils.getActorId(actorDefinition);

        long startTime = System.currentTimeMillis();

        TaskContainer taskContainer = taskServer.poll(actorDefinition);

        String queueName = TransportUtils.createQueueName(actorId, TransportUtils.getTaskList(taskContainer));

        long invocationTime = System.currentTimeMillis() - startTime;
        Metric pollMetric = metricsFactory.getInstance(MetricName.POLL.getValue());
        pollMetric.mark(queueName, invocationTime);
        pollMetric.mark(MetricName.POLL.getValue(), invocationTime);

        if (taskContainer!=null) {
            Metric successPollMetric = metricsFactory.getInstance(MetricName.SUCCESSFUL_POLL.getValue());
            successPollMetric.mark(queueName, invocationTime);
            successPollMetric.mark(MetricName.SUCCESSFUL_POLL.getValue(), invocationTime);
        }

        return taskContainer;
    }

    @Override
    public void release(DecisionContainer taskResult) {

        String actorId = taskResult.getActorId();

        long startTime = System.currentTimeMillis();

        taskServer.release(taskResult);

        long invocationTime = System.currentTimeMillis() - startTime;

        Metric releaseMetric = metricsFactory.getInstance(MetricName.RELEASE.getValue());
        releaseMetric.mark(actorId, invocationTime);
        releaseMetric.mark(MetricName.RELEASE.getValue(), invocationTime);

        Metric execTimeMetric = metricsFactory.getInstance(MetricName.EXECUTION_TIME.getValue());
        execTimeMetric.mark(actorId, taskResult.getExecutionTime());
        execTimeMetric.mark(MetricName.EXECUTION_TIME.getValue(), taskResult.getExecutionTime());

        if (taskResult.containsError()) {
            Metric errMetric = metricsFactory.getInstance(MetricName.ERROR_DECISION.getValue());
            errMetric.mark(actorId, taskResult.getExecutionTime());
            errMetric.mark(MetricName.ERROR_DECISION.getValue(), taskResult.getExecutionTime());
        }

    }

}
