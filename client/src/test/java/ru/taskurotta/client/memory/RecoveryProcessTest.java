package ru.taskurotta.client.memory;

import org.junit.Test;
import ru.taskurotta.core.Task;
import ru.taskurotta.service.dependency.GeneralDependencyService;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.dependency.links.MemoryGraphDao;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.util.ActorUtils;

import java.util.HashMap;
import java.util.UUID;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * User: stukushin
 * Date: 16.08.13
 * Time: 16:43
 */
public class RecoveryProcessTest extends AbstractTestStub {

    @Test
    public void testRecoveryProcessFromStartTask() throws Exception {
        UUID startTaskId = UUID.randomUUID();

        // start process
        startProcess(deciderTask(startTaskId, TaskType.DECIDER_START, "A"));

        // check start task in queue
        assertTrue(isTaskInQueue(DECIDER_ACTOR_DEF, startTaskId, processId));

        // clean tasks and graph
        memoryQueueService.simulateDataLoss();// = new MemoryQueueService(0);
        dependencyService = new GeneralDependencyService(new MemoryGraphDao());
        recoveryProcessService.setDependencyService(dependencyService);

        // check no tasks in queue
        assertFalse(isTaskInQueue(DECIDER_ACTOR_DEF, startTaskId, processId));
        // check no graph for process
        assertNull(dependencyService.getGraph(processId));

        // recovery process
        recoveryProcessService.restartProcess(processId);

        assertFalse(isTaskInQueue(DECIDER_ACTOR_DEF, startTaskId, processId));

        memoryQueueService.poll(ActorUtils.getActorId(DECIDER_ACTOR_DEF), null);

        // recovery process
        recoveryProcessService.restartProcess(processId);

        // check start task in queue
        assertTrue(isTaskInQueue(DECIDER_ACTOR_DEF, startTaskId, processId));
        // check not null graph for process
        assertNotNull(dependencyService.getGraph(processId));
    }

    @Test
    public void testRecoveryProcessFromIncompleteTasks() throws Exception {
        UUID startTaskId = UUID.randomUUID();
        UUID workerTaskId = UUID.randomUUID();

        // start process
        startProcess(deciderTask(startTaskId, TaskType.DECIDER_START, "A"));

        // poll start process task
        pollDeciderTask(startTaskId);

        Task workerTask = workerTask(workerTaskId, TaskType.WORKER, "A", new Object[]{});

        // release start process task and add worker task
        release(startTaskId, null, workerTask);

        // check worker task in queue
        assertTrue(isTaskPresent(workerTaskId, processId));

        // clean tasks from queues
        memoryQueueService.simulateDataLoss();
        //memoryQueueService = new MemoryQueueService(0);
        // check no tasks in queue
        assertFalse(isTaskInQueue(WORKER_ACTOR_DEF, startTaskId, processId));

        // check not finished items in graph
        assertFalse(dependencyService.getGraph(processId).getNotFinishedItems().isEmpty());

        // recovery process
        recoveryProcessService.restartProcess(processId);

        assertFalse(isTaskInQueue(WORKER_ACTOR_DEF, startTaskId, processId));

        memoryQueueService.poll(ActorUtils.getActorId(WORKER_ACTOR_DEF), null);

        // recovery process
        recoveryProcessService.restartProcess(processId);

        // check tasks in queue
        assertTrue(isTaskInQueue(WORKER_ACTOR_DEF, workerTaskId, processId));
    }

    @Test
    public void testRecoveryProcessFromGraphWithoutNotFinishedTasks() throws Exception {
        UUID startTaskId = UUID.randomUUID();
        UUID workerTaskId = UUID.randomUUID();

        // start process
        startProcess(deciderTask(startTaskId, TaskType.DECIDER_START, "A"));

        // poll start process task
        pollDeciderTask(startTaskId);

        Task workerTask = workerTask(workerTaskId, TaskType.WORKER, "A", new Object[]{});

        // release start process task and add worker task
        release(startTaskId, null, workerTask);

        // check worker task in queue
        assertTrue(isTaskPresent(workerTaskId, processId));

        // clean tasks from queues
        memoryQueueService.simulateDataLoss();

        // check no tasks in queue
        assertFalse(isTaskInQueue(WORKER_ACTOR_DEF, startTaskId, processId));

        // reset not finished items
        dependencyService.changeGraph(new GraphDao.Updater() {
            @Override
            public UUID getProcessId() {
                return processId;
            }

            @Override
            public boolean apply(Graph graph) {
                graph.setVersion(graph.getVersion() + 1);
                graph.setNotFinishedItems(new HashMap<UUID, Long>());
                return true;
            }
        });

        memoryQueueService.poll(ActorUtils.getActorId(WORKER_ACTOR_DEF), null);

        // recovery process
        recoveryProcessService.restartProcess(processId);

        // check tasks in queue
        assertTrue(isTaskInQueue(WORKER_ACTOR_DEF, workerTaskId, processId));
    }
}
