package ru.taskurotta.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.statistics.MetricsFactory;
import ru.taskurotta.service.statistics.MetricName;
import ru.taskurotta.service.statistics.datalistener.NumberDataListener;
import ru.taskurotta.service.statistics.metrics.Metric;
import ru.taskurotta.service.statistics.metrics.TimeConstants;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * TaskServer wrapper delegating method calls to enclosed server with metrics data collect operations
 * Date: 27.08.13 14:39
 */
public class MetricsTaskServer implements TaskServer {

    private TaskServer taskServer;
    private MetricsFactory metricsFactory;
    private NumberDataListener numberDataListener;

    private static final Logger logger = LoggerFactory.getLogger(MetricsTaskServer.class);

    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread result = new Thread(r);
            result.setDaemon(true);
            result.setName("memory-data-gatherer");
            return result;
        }
    });

    public MetricsTaskServer(TaskServer taskServer, MetricsFactory metricsFactory, NumberDataListener numberDataListener, int metricsPeriodSeconds) {
        this.taskServer = taskServer;
        this.metricsFactory = metricsFactory;
        this.numberDataListener = numberDataListener;

        //Queue statistics for metrics
        int dataPointsCount = TimeConstants.SECONDS_IN_24_HOURS/Long.valueOf(metricsPeriodSeconds).intValue();//number of points to cover 24 hours period.
        scheduledExecutorService.scheduleAtFixedRate(new MemoryDataFlusher(dataPointsCount), 0, metricsPeriodSeconds, TimeUnit.SECONDS);
    }

    class MemoryDataFlusher implements Runnable {
        private int dataSize = 0;

        private static final String FREE_MEM = "free";
        private static final String TOTAL_MEM = "total";

        MemoryDataFlusher (int dataSize) {
            this.dataSize = dataSize;
        }

        @Override
        public void run() {
            long free = -1l;
            long total = -1l;
            try {
                if (numberDataListener != null) {
                    Runtime runtime = Runtime.getRuntime();
                    free = runtime.freeMemory();
                    total = runtime.totalMemory();
                    numberDataListener.handleNumberData(MetricName.MEMORY.getValue(), FREE_MEM, free,
                            System.currentTimeMillis(), dataSize);

                    numberDataListener.handleNumberData(MetricName.MEMORY.getValue(), TOTAL_MEM, total,
                            System.currentTimeMillis(), dataSize);

                    numberDataListener.handleNumberData(MetricName.MEMORY.getValue(), MetricName.MEMORY.getValue(), total-free,
                            System.currentTimeMillis(), dataSize);

                }
                logger.debug("Memory data successfully flushed, total[{}], free[{}]", total, free);

            } catch (Throwable e) {
                logger.error("MemoryDataFlusher iteration failed", e);
            }
        }

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

        long invocationTime = System.currentTimeMillis() - startTime;
        Metric pollMetric = metricsFactory.getInstance(MetricName.POLL.getValue());
        pollMetric.mark(actorId, invocationTime);
        pollMetric.mark(MetricName.POLL.getValue(), invocationTime);

        if (taskContainer!=null) {
            Metric successPollMetric = metricsFactory.getInstance(MetricName.SUCCESSFUL_POLL.getValue());
            successPollMetric.mark(actorId, invocationTime);
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
