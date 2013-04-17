package ru.taskurotta.server;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:04 PM
 */
public class GeneralTaskServer implements TaskServer {

    private final static Logger logger = LoggerFactory.getLogger(GeneralTaskServer.class);

    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;


    public GeneralTaskServer(BackendBundle backendBundle) {
        this.processBackend = backendBundle.getProcessBackend();
        this.taskBackend = backendBundle.getTaskBackend();
        this.queueBackend = backendBundle.getQueueBackend();
        this.dependencyBackend = backendBundle.getDependencyBackend();
        this.configBackend = backendBundle.getConfigBackend();
    }

    public GeneralTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        this.processBackend = processBackend;
        this.taskBackend = taskBackend;
        this.queueBackend = queueBackend;
        this.dependencyBackend = dependencyBackend;
        this.configBackend = configBackend;
    }

    @Override
    public void startProcess(TaskContainer task) {

        // some consistence check
        if (!task.getType().equals(TaskType.DECIDER_START)) {
            // TODO: send error to client
            throw new IllegalStateException("Can not start process. Task should be type of " + TaskType.DECIDER_START);
        }

        // registration of new process
        // atomic statement
        processBackend.startProcess(task);

        // inform taskBackend about new process
        // idempotent statement
        taskBackend.startProcess(task);

        // inform dependencyBackend about new process
        // idempotent statement
        dependencyBackend.startProcess(task);

        // we assume that new process task has no dependencies and it is ready to enqueue.
        // idempotent statement
        enqueueTask(task.getTaskId(), task.getActorId(), task.getStartTime());


        processBackend.startProcessCommit(task);
    }


    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        if (configBackend.isActorBlocked(ActorUtils.getActorId(actorDefinition))) {
            // TODO: ? We should inform client about block. It should catch exception and try to sleep some time ?
            // TODO: ? Or we should sleep 60 seconds as usual ?
            return null;
        }

        // atomic statement
        UUID taskId = queueBackend.poll(actorDefinition.getFullName(), null);

        if (taskId == null) {
            return null;
        }

        // idempotent statement
        final TaskContainer taskContainer = taskBackend.getTaskToExecute(taskId);

        queueBackend.pollCommit(actorDefinition.getFullName(), taskId);

        return taskContainer;
    }


    @Override
    public void release(DecisionContainer taskDecision) {

        // save it firstly
        taskBackend.addDecision(taskDecision);

        UUID taskId = taskDecision.getTaskId();

        // if Error
        if (taskDecision.containsError()) {
            final boolean isShouldBeRestarted = taskDecision.getRestartTime() != TaskDecision.NO_RESTART;

            // enqueue task immediately if needed
            if (isShouldBeRestarted) {

                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer asyncTask = taskBackend.getTask(taskId);
                logger.debug("Error task enqueued again, taskId [{}]", taskId);
                enqueueTask(taskId, asyncTask.getActorId(), taskDecision.getRestartTime());
            }

            taskBackend.addDecisionCommit(taskDecision);

            return;
        }


        // idempotent statement
        DependencyDecision dependencyDecision = dependencyBackend.applyDecision(taskDecision);

        logger.debug("release() received dependencyDecision = [{}]", dependencyDecision);

        if (dependencyDecision.isFail()) {

            logger.debug("release() failed dependencyDecision. release() should be retried after " +
                    "RELEASE_TIMEOUT");

            // leave release() method.
            // RELEASE_TIMEOUT should be automatically fired
            return;
        }

        List<UUID> readyTasks = dependencyDecision.getReadyTasks();

        if (readyTasks != null) {

            for (UUID taskId2Queue : readyTasks) {

                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer asyncTask = taskBackend.getTask(taskId2Queue);
                enqueueTask(taskId2Queue, asyncTask.getActorId(), asyncTask.getStartTime());
            }

        }

        if (dependencyDecision.isProcessFinished()) {
            processBackend.finishProcess(dependencyDecision.getFinishedProcessId(),
                    dependencyDecision.getFinishedProcessValue());
        }

        taskBackend.addDecisionCommit(taskDecision);
    }

    private void enqueueTask(UUID taskId, String actorId, long startTime) {

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }
        queueBackend.enqueueItem(actorId, taskId, startTime, null);
    }

}
