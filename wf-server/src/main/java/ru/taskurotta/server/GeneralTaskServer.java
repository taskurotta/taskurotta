package ru.taskurotta.server;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.StorageBackend;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:04 PM
 */
public class GeneralTaskServer implements TaskServer{

    private StorageBackend storageBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;


    public GeneralTaskServer(BackendBundle backendBundle) {
        this.storageBackend = backendBundle.getStorageBackend();
        this.queueBackend = backendBundle.getQueueBackend();
        this.dependencyBackend = backendBundle.getDependencyBackend();
        this.configBackend = backendBundle.getConfigBackend();
    }

    public GeneralTaskServer(StorageBackend storageBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        this.storageBackend = storageBackend;
        this.queueBackend = queueBackend;
        this.dependencyBackend = dependencyBackend;
        this.configBackend = configBackend;
    }    
    
	@Override
    public void startProcess(TaskContainer task) {

        // some consistence check
        if (!task.getTarget().getType().equals(TaskType.DECIDER_START)) {
            // TODO: send error to client
            throw new IllegalStateException("Can not start process. Task should be type of " + TaskType.DECIDER_START);
        }

        // registration of new process
        storageBackend.addProcess(task);

        final TaskTarget taskTarget = task.getTarget();

        // idempotent statement
        dependencyBackend.startProcess(task);

        // we assume that new process task has no dependencies and it is ready to enqueue.
        // idempotent statement
        enqueueTask(task.getTaskId(), taskTarget.getName(), taskTarget.getVersion(), task.getStartTime());
    }


    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        if (configBackend.isActorBlocked(actorDefinition)) {
            // TODO: ? We should inform client about block. It should catch exception and try to sleep some time ?
            // TODO: ? Or we should sleep 60 seconds as usual ?
            return null;
        }

        // Task can be polled but not marked to "process" state (storageBackend.getTaskToExecute(taskId))
        // Should we use pollCommit ?
        UUID taskId = queueBackend.poll(actorDefinition);

        if (taskId == null) {
            return null;
        }

        // idempotent statement
        final TaskContainer taskContainer = storageBackend.getTaskToExecute(taskId);

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

            storageBackend.addError(taskId, errorContainer, isShouldBeRestarted);

            // enqueue task immediately if needed
            if (isShouldBeRestarted) {

                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer asyncTask = storageBackend.getTask(taskId);
                enqueueTask(taskId, asyncTask.getTarget().getName(), asyncTask.getTarget().getVersion(),
                        errorContainer.getRestartTime());
            }

            storageBackend.addErrorCommit(taskId);

            return;
        }

        // if Success
        storageBackend.addDecision(taskDecision);

        // idempotent statement
        DependencyDecision dependencyDecision = dependencyBackend.analyzeDecision(taskDecision);
        List<UUID> readyTasks = dependencyDecision.getReadyTasks();

        if (readyTasks != null) {

            for (UUID taskId2Queue : readyTasks) {

                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer asyncTask = storageBackend.getTask(taskId2Queue);
                enqueueTask(taskId2Queue, asyncTask.getTarget().getName(), asyncTask.getTarget().getVersion(),
                        asyncTask.getStartTime());
            }

        }

        storageBackend.addDecisionCommit(taskId, dependencyDecision.isProcessFinished());
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
