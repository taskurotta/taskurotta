package ru.taskurotta.server;

import ru.taskurotta.service.statistics.MetricFactory;
import ru.taskurotta.service.statistics.MetricName;
import ru.taskurotta.service.statistics.metrics.Metric;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

/**
 * User: stukushin
 * Date: 27.08.13
 * Time: 14:39
 */
public class MetricsTaskServer implements TaskServer {

    private TaskServer taskServer;
    private MetricFactory metricsFactory;

    public MetricsTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
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

    public void setMetricsFactory(MetricFactory metricsFactory) {
        this.metricsFactory = metricsFactory;
    }
}
