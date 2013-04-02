package ru.taskurotta.server.recovery;

import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.StorageBackend;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * First pre-mega-supa-alfa of full recovery process;
 * <p/>
 * User: romario
 * Date: 4/1/13
 * Time: 8:39 PM
 */
public class FullRecovery {

    private StorageBackend storageBackend;
    private DependencyBackend dependencyBackend;
    private QueueBackend queueBackend;

    public void run() {

        // This is stupid but good for pre-mega-supa-alfa :)
        List<TaskContainer> allRunProcesses = storageBackend.getAllRunProcesses();

        for (TaskContainer taskContainer : allRunProcesses) {

            List<UUID> tasksToQueueList = new LinkedList<UUID>();

            // emulate start of process
            dependencyBackend.startProcess(taskContainer);
            tasksToQueueList.add(taskContainer.getTaskId());

            // Also stupid :)
            List<DecisionContainer> decisionContainers = storageBackend.getAllTaskDecisions(taskContainer.getTaskId());

            // ? Should we put start task to dependencyBackend firstly ?

            for (DecisionContainer decisionContainer : decisionContainers) {

                // remove resolved task
                tasksToQueueList.remove(decisionContainer.getTaskId());
                // add all new ready tasks to queue
                DependencyDecision dependencyDecision = dependencyBackend.analyzeDecision(decisionContainer);

                List<UUID> readyTasks = dependencyDecision.getReadyTasks();
                if (readyTasks != null) {
                    tasksToQueueList.addAll(readyTasks);
                }
            }

            for (UUID taskToQueueId : tasksToQueueList) {

                TaskContainer task2Queue = storageBackend.getTask(taskToQueueId);

                queueBackend.enqueueItem(
                        ActorDefinition.valueOf(
                                task2Queue.getTarget().getName(),
                                task2Queue.getTarget().getVersion()
                        ),
                        taskToQueueId,
                        task2Queue.getStartTime()); // This time may be shifted by RetryPolicy
            }
        }

    }


}
