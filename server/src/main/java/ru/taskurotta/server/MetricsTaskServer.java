package ru.taskurotta.server;

import ru.taskurotta.backend.statistics.MetricFactory;
import ru.taskurotta.backend.statistics.metrics.Metric;
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

    public static final String START_PROCESS = "startProcess";
    public static final String POLL = "poll";
    public static final String SUCCESSFUL_POLL = "successfulPoll";
    public static final String RELEASE = "release";
    public static final String EXECUTION_TIME = "executionTime";
    public static final String ERROR_DECISION = "errorDecision";

    public MetricsTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

    @Override
    public void startProcess(TaskContainer task) {

        String actorId = task.getActorId();

        long startTime = System.currentTimeMillis();

        taskServer.startProcess(task);

        long invocationTime = System.currentTimeMillis()-startTime;
        Metric startProcessMetric = metricsFactory.getInstance(START_PROCESS);
        startProcessMetric.mark(actorId, invocationTime);
        startProcessMetric.mark(START_PROCESS, invocationTime);

    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        String actorId = ActorUtils.getActorId(actorDefinition);

        long startTime = System.currentTimeMillis();

        TaskContainer taskContainer = taskServer.poll(actorDefinition);

        long invocationTime = System.currentTimeMillis() - startTime;
        Metric pollMetric = metricsFactory.getInstance(POLL);
        pollMetric.mark(actorId, invocationTime);
        pollMetric.mark(POLL, invocationTime);

        if (taskContainer!=null) {
            Metric successPollMetric = metricsFactory.getInstance(SUCCESSFUL_POLL);
            successPollMetric.mark(actorId, invocationTime);
            successPollMetric.mark(SUCCESSFUL_POLL, invocationTime);
        }

        return taskContainer;
    }

    @Override
    public void release(DecisionContainer taskResult) {

        String actorId = taskResult.getActorId();

        long startTime = System.currentTimeMillis();

        taskServer.release(taskResult);

        long invocationTime = System.currentTimeMillis() - startTime;

        Metric releaseMetric = metricsFactory.getInstance(RELEASE);
        releaseMetric.mark(actorId, invocationTime);
        releaseMetric.mark(RELEASE, invocationTime);

        Metric execTimeMetric = metricsFactory.getInstance(EXECUTION_TIME);
        execTimeMetric.mark(actorId, taskResult.getExecutionTime());
        execTimeMetric.mark(EXECUTION_TIME, taskResult.getExecutionTime());

        if (taskResult.containsError()) {
            Metric errMetric = metricsFactory.getInstance(ERROR_DECISION);
            errMetric.mark(actorId, taskResult.getExecutionTime());
            errMetric.mark(ERROR_DECISION, taskResult.getExecutionTime());
        }

    }

    public void setMetricsFactory(MetricFactory metricsFactory) {
        this.metricsFactory = metricsFactory;
    }
}
