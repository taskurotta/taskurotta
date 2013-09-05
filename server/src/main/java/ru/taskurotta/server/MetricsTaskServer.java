package ru.taskurotta.server;

import ru.taskurotta.backend.statistics.MetricsManager;
import ru.taskurotta.backend.statistics.datalisteners.DataListener;
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
    private MetricsManager metricsManager;
    private DataListener dataListener;

    public static final String START_PROCESS = "startProcess";
    public static final String POLL = "poll";
    public static final String RELEASE = "release";
    public static final String EXECUTION_TIME = "executionTime";
    public static final String ERROR_DECISION = "errorDecision";

    public MetricsTaskServer(TaskServer taskServer, MetricsManager metricsManager, DataListener dataListener) {
        this.taskServer = taskServer;
        this.metricsManager = metricsManager;
        this.dataListener = dataListener;
    }

    @Override
    public void startProcess(TaskContainer task) {

        String actorId = task.getActorId();

        long startTime = System.currentTimeMillis();

        taskServer.startProcess(task);

        metricsManager.mark(START_PROCESS, actorId, System.currentTimeMillis() - startTime, dataListener);
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        String actorId = ActorUtils.getActorId(actorDefinition);

        long startTime = System.currentTimeMillis();

        TaskContainer taskContainer = taskServer.poll(actorDefinition);

        metricsManager.mark(POLL, actorId, System.currentTimeMillis() - startTime, dataListener);

        return taskContainer;
    }

    @Override
    public void release(DecisionContainer taskResult) {

        String actorId = taskResult.getActorId();

        long startTime = System.currentTimeMillis();

        taskServer.release(taskResult);

        metricsManager.mark(RELEASE, actorId, System.currentTimeMillis() - startTime, dataListener);

        metricsManager.mark(EXECUTION_TIME, actorId, taskResult.getExecutionTime(), dataListener);

        if (taskResult.containsError()) {
            metricsManager.mark(ERROR_DECISION, actorId, taskResult.getExecutionTime(), dataListener);
        }
    }

    public void shutdown() {
        metricsManager.shutdown();
    }
}
