package ru.taskurotta.server;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.model.QueuedItem;
import ru.taskurotta.backend.storage.StorageBackend;
import ru.taskurotta.backend.storage.model.AsyncProcess;
import ru.taskurotta.backend.storage.model.AsyncTask;
import ru.taskurotta.backend.storage.model.AsyncTaskError;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.server.transport.BackendConverter;
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.server.transport.ErrorContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:04 PM
 */
public class GeneralTaskServer implements TaskServer {

    private StorageBackend storageBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;

    @Override
    public void startProcess(TaskContainer task) {

        // some consistence check
        if (!task.getTarget().getType().equals(TaskType.DECIDER_START)) {
            // TODO: send error to client
            throw new IllegalStateException("Can not start process. Task should be type of " + TaskType.DECIDER_START);
        }

        // registration of new process
        final AsyncProcess asyncProcess = BackendConverter.toAsyncProcess(task);
        storageBackend.createNewProcess(asyncProcess);

        final TaskTarget taskTarget = task.getTarget();

        // we assume that new process task has no dependencies and it is ready to enqueue.
        enqueueTask(taskTarget.getName(), taskTarget.getVersion(), task.getTaskId(), task.getStartTime());
    }


    private void enqueueTask(String actorName, String actorVersion, UUID taskId, long startTime) {

        final ActorDefinition actorDefinition = ActorDefinition.valueOf(actorName, actorVersion);

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }
        final QueuedItem queuedItem = new QueuedItem(taskId, startTime);
        queueBackend.enqueueItem(actorDefinition, queuedItem);
    }


    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        if (configBackend.isActorBlocked(actorDefinition)) {
            // TODO: ? We should inform client about block. It should catch exception and try to sleep some time ?
            // TODO: ? Or we should sleep 60 seconds as usual ?
            return null;
        }

        QueuedItem queuedItem = queueBackend.poll(actorDefinition);

        if (queuedItem == null) {
            return null;
        }

        final AsyncTask asyncTask = storageBackend.getTaskToExecute(queuedItem.getTaskId());
        final TaskContainer taskContainer = BackendConverter.toTaskContainer(asyncTask);

        return taskContainer;
    }

    @Override
    public void release(DecisionContainer taskResult) {

        // ? should full DecisionContainer be logged ?

        if (taskResult.isError()) {
            final ErrorContainer errorContainer = taskResult.getErrorContainer();
            final AsyncTaskError asyncTaskError = BackendConverter.toAsyncTaskError(taskResult.getTaskId(),
                    errorContainer);
            final boolean isShouldBeRestarted = errorContainer.isShouldBeRestarted();

            storageBackend.logError(asyncTaskError, isShouldBeRestarted);

            // enqueue task immediately if needed
            if (isShouldBeRestarted) {

                UUID taskId = taskResult.getTaskId();

                // WARNING: This id not optimal code. We are getting whole task only for name and version values.
                AsyncTask asyncTask = storageBackend.getTask(taskId);
                enqueueTask(asyncTask.getName(), asyncTask.getVersion(), taskResult.getTaskId(),
                        errorContainer.getRestartTime());
            }

            return;
        }

        // storageBackend: register result and new tasks
        // dependencyBackend: getReadyTasks
        // queueBackend: enqueueItem
    }
}
