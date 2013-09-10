package ru.taskurotta.server;

import ru.taskurotta.backend.statistics.ActorMetricsManager;
import ru.taskurotta.backend.statistics.GeneralMetricsManager;
import ru.taskurotta.backend.statistics.datalisteners.DataListener;
import ru.taskurotta.backend.statistics.datalisteners.LoggerActorDataListener;
import ru.taskurotta.backend.statistics.datalisteners.LoggerDataListener;
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
    private GeneralMetricsManager generalMetricsManager;
    private ActorMetricsManager actorMetricsManager;
    private DataListener dataListener;

    public static final String START_PROCESS = "startProcess";
    public static final String POLL = "poll";
    public static final String RELEASE = "release";
    public static final String EXECUTION_TIME = "executionTime";
    public static final String ERROR_DECISION = "errorDecision";

    public MetricsTaskServer(TaskServer taskServer, GeneralMetricsManager generalMetricsManager, ActorMetricsManager actorMetricsManager,DataListener dataListener) {
        this.taskServer = taskServer;
        this.generalMetricsManager = generalMetricsManager;
        this.actorMetricsManager = actorMetricsManager;
        this.dataListener = dataListener;
    }

    @Override
    public void startProcess(TaskContainer task) {

        String actorId = task.getActorId();

        long startTime = System.currentTimeMillis();

        taskServer.startProcess(task);

        generalMetricsManager.mark(START_PROCESS, System.currentTimeMillis() - startTime, dataListener);

        actorMetricsManager.mark(actorId, START_PROCESS, System.currentTimeMillis() - startTime, new LoggerActorDataListener(actorId));
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        String actorId = ActorUtils.getActorId(actorDefinition);

        long startTime = System.currentTimeMillis();

        TaskContainer taskContainer = taskServer.poll(actorDefinition);

        generalMetricsManager.mark(POLL, System.currentTimeMillis() - startTime, dataListener);

        actorMetricsManager.mark(actorId, POLL, System.currentTimeMillis() - startTime, new LoggerActorDataListener(actorId));

        return taskContainer;
    }

    @Override
    public void release(DecisionContainer taskResult) {

        String actorId = taskResult.getActorId();

        long startTime = System.currentTimeMillis();

        taskServer.release(taskResult);

        generalMetricsManager.mark(RELEASE, System.currentTimeMillis() - startTime, dataListener);

        actorMetricsManager.mark(actorId, RELEASE, System.currentTimeMillis() - startTime, new LoggerActorDataListener(actorId));

        generalMetricsManager.mark(EXECUTION_TIME, taskResult.getExecutionTime(), dataListener);

        actorMetricsManager.mark(actorId, EXECUTION_TIME, System.currentTimeMillis() - startTime, new LoggerActorDataListener(actorId));

        if (taskResult.containsError()) {
            generalMetricsManager.mark(ERROR_DECISION, taskResult.getExecutionTime(), dataListener);

            actorMetricsManager.mark(actorId, ERROR_DECISION, System.currentTimeMillis() - startTime, new LoggerActorDataListener(actorId));
        }
    }

    public void shutdown() {
        generalMetricsManager.shutdown();
    }
}
