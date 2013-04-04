package ru.taskurotta.server;

import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.util.ActorDefinition;

import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:04 PM
 */
public class GeneralTaskServer implements TaskServer {

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


    @Override
    public void startProcess(TaskContainer task) {

        // some consistence check
        if (!task.getTarget().getType().equals(TaskType.DECIDER_START)) {
            // TODO: send error to client
            throw new IllegalStateException("Can not start process. Task should be type of " + TaskType.DECIDER_START);
        }

        // registration of new process
        // atomic statement
        processBackend.startProcess(task);

        // inform taskBackend about new process
        // idempotent statement
        taskBackend.startProcess(task);

        final TaskTarget taskTarget = task.getTarget();

        // inform dependencyBackend about new process
        // idempotent statement
        dependencyBackend.startProcess(task);

        // we assume that new process task has no dependencies and it is ready to enqueue.
        // idempotent statement
        enqueueTask(task.getTaskId(), taskTarget.getName(), taskTarget.getVersion(), task.getStartTime());


        processBackend.startProcessCommit(task.getTaskId());
    }


    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        if (configBackend.isActorBlocked(actorDefinition)) {
            // TODO: ? We should inform client about block. It should catch exception and try to sleep some time ?
            // TODO: ? Or we should sleep 60 seconds as usual ?
            return null;
        }

        // atomic statement
        UUID taskId = queueBackend.poll(actorDefinition);

        if (taskId == null) {
            return null;
        }

        // idempotent statement
        final TaskContainer taskContainer = taskBackend.getTaskToExecute(taskId);

        queueBackend.pollCommit(taskId);

        return taskContainer;
    }


    @Override
    public void release(DecisionContainer taskDecision) {

        // ? should full DecisionContainer be logged ?

        UUID taskId = taskDecision.getTaskId();

        // if Error
        if (taskDecision.isError()) {
            final ErrorContainer errorContainer = taskDecision.getErrorContainer();
            final boolean isShouldBeRestarted = errorContainer.isShouldBeRestarted();

            taskBackend.addError(taskId, errorContainer, isShouldBeRestarted);

            // enqueue task immediately if needed
            if (isShouldBeRestarted) {

                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer asyncTask = taskBackend.getTask(taskId);
                enqueueTask(taskId, asyncTask.getTarget().getName(), asyncTask.getTarget().getVersion(),
                        errorContainer.getRestartTime());
            }

            taskBackend.addErrorCommit(taskId);

            return;
        }

        // if Success
        taskBackend.addDecision(taskDecision);

        // idempotent statement
        DependencyDecision dependencyDecision = dependencyBackend.analyzeDecision(taskDecision);
        List<UUID> readyTasks = dependencyDecision.getReadyTasks();

        if (readyTasks != null) {

            for (UUID taskId2Queue : readyTasks) {

                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer asyncTask = taskBackend.getTask(taskId2Queue);
                enqueueTask(taskId2Queue, asyncTask.getTarget().getName(), asyncTask.getTarget().getVersion(),
                        asyncTask.getStartTime());
            }

        }

        if (dependencyDecision.isProcessFinished()) {
            processBackend.finishProcess(dependencyDecision.getFinishedProcessId(),
                    dependencyDecision.getFinishedProcessValue());
        }

        taskBackend.addDecisionCommit(taskId);
    }

    private void enqueueTask(UUID taskId, String actorName, String actorVersion, long startTime) {

        final ActorDefinition actorDefinition = ActorDefinition.valueOf(actorName, actorVersion);

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }
        queueBackend.enqueueItem(actorDefinition, taskId, startTime);
    }

}
