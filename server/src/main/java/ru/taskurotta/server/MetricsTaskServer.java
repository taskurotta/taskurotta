package ru.taskurotta.server;

import ru.taskurotta.backend.statistics.MetricsManager;
import ru.taskurotta.backend.statistics.datalisteners.DataListener;
import ru.taskurotta.backend.statistics.metrics.Counter;
import ru.taskurotta.backend.statistics.metrics.CheckPoint;
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

    private static CheckPoint startProcessCheckPoint = null;
    private static CheckPoint pollCheckPoint = null;
    private static CheckPoint releaseCheckPoint = null;

    private static CheckPoint taskExecutionTimeCheckPoint = null;

    private static Counter startProcessCounter = null;
    private static Counter pollCounter = null;
    private static Counter releaseCounter = null;

    private static Counter errorDecisionCounter = null;

    public MetricsTaskServer(TaskServer taskServer, DataListener dataListener) {
        this.taskServer = taskServer;

        startProcessCheckPoint = MetricsManager.createCheckPoint("startProcess", dataListener);
        pollCheckPoint = MetricsManager.createCheckPoint("poll", dataListener);
        releaseCheckPoint = MetricsManager.createCheckPoint("release", dataListener);

        taskExecutionTimeCheckPoint = MetricsManager.createCheckPoint("executionTime", dataListener);

        startProcessCounter = MetricsManager.createCounter("startProcess", dataListener);
        pollCounter = MetricsManager.createCounter("poll", dataListener);
        releaseCounter = MetricsManager.createCounter("release", dataListener);

        errorDecisionCounter = MetricsManager.createCounter("errorDecision", dataListener);
    }

    @Override
    public void startProcess(TaskContainer task) {

        String actorId = task.getActorId();

        startProcessCounter.mark(actorId);

        long startTime = System.currentTimeMillis();

        taskServer.startProcess(task);

        startProcessCheckPoint.mark(actorId, System.currentTimeMillis() - startTime);
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        String actorId = ActorUtils.getActorId(actorDefinition);

        pollCounter.mark(actorId);

        long startTime = System.currentTimeMillis();

        TaskContainer taskContainer = taskServer.poll(actorDefinition);

        pollCheckPoint.mark(actorId, System.currentTimeMillis() - startTime);

        return taskContainer;
    }

    @Override
    public void release(DecisionContainer taskResult) {

        String actorId = taskResult.getActorId();

        releaseCounter.mark(actorId);

        if (taskResult.containsError()) {
            errorDecisionCounter.mark(actorId);
        }

        long startTime = System.currentTimeMillis();

        taskServer.release(taskResult);

        releaseCheckPoint.mark(actorId, System.currentTimeMillis() - startTime);

        taskExecutionTimeCheckPoint.mark(actorId, taskResult.getExecutionTime());
    }

    public void shutdown() {
        MetricsManager.shutdown();
    }
}
