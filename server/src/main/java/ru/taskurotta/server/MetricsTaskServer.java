package ru.taskurotta.server;

import ru.taskurotta.backend.statistics.MetricFactory;
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
        metricsFactory.getInstance(START_PROCESS).mark(actorId, invocationTime);

    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        String actorId = ActorUtils.getActorId(actorDefinition);

        long startTime = System.currentTimeMillis();

        TaskContainer taskContainer = taskServer.poll(actorDefinition);

        long invocationTime = System.currentTimeMillis() - startTime;
        metricsFactory.getInstance(POLL).mark(actorId, invocationTime);

        if(taskContainer!=null) {
            metricsFactory.getInstance(SUCCESSFUL_POLL).mark(actorId, invocationTime);
        }

        return taskContainer;
    }

    @Override
    public void release(DecisionContainer taskResult) {

        String actorId = taskResult.getActorId();

        long startTime = System.currentTimeMillis();

        taskServer.release(taskResult);

        long invocationTime = System.currentTimeMillis() - startTime;

        metricsFactory.getInstance(RELEASE).mark(actorId, invocationTime);
        metricsFactory.getInstance(EXECUTION_TIME).mark(actorId, taskResult.getExecutionTime());

        if (taskResult.containsError()) {
            metricsFactory.getInstance(ERROR_DECISION).mark(actorId, taskResult.getExecutionTime());
        }

    }

    public void setMetricsFactory(MetricFactory metricsFactory) {
        this.metricsFactory = metricsFactory;
    }
}
