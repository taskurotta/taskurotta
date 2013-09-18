package ru.taskurotta.client.memory;

import org.junit.Test;
import ru.taskurotta.backend.dependency.GeneralDependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.dependency.links.MemoryGraphDao;
import ru.taskurotta.backend.queue.MemoryQueueBackend;
import ru.taskurotta.backend.recovery.RecoveryTask;
import ru.taskurotta.core.Task;
import ru.taskurotta.transport.model.TaskType;

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
public class TestRecoveryProcess extends AbstractTestStub {

    @Test
    public void testRecoveryProcessFromStartTask() throws Exception {
        UUID startTaskId = UUID.randomUUID();

        // start process
        startProcess(deciderTask(startTaskId, TaskType.DECIDER_START, "A"));

        // check start task in queue
        assertTrue(isTaskInQueue(DECIDER_ACTOR_DEF, startTaskId, processId));

        // clean tasks and graph
        memoryQueueBackend = new MemoryQueueBackend(0);
        dependencyBackend = new GeneralDependencyBackend(new MemoryGraphDao());

        // check no tasks in queue
        assertFalse(isTaskInQueue(DECIDER_ACTOR_DEF, startTaskId, processId));
        // check no graph for process
        assertNull(dependencyBackend.getGraph(processId));

        // recovery process
        new RecoveryTask(memoryQueueBackend, memoryQueueBackendStatistics, dependencyBackend, taskDao, backendBundle.getProcessBackend(), backendBundle.getTaskBackend(), 1l, processId).call();

        // check start task in queue
        assertTrue(isTaskInQueue(DECIDER_ACTOR_DEF, startTaskId, processId));
        // check not null graph for process
        assertNotNull(dependencyBackend.getGraph(processId));
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
        release(startTaskId, null, new Task[] {workerTask});

        // check worker task in queue
        assertTrue(isTaskPresent(workerTaskId, processId));

        // clean tasks from queues
        memoryQueueBackend = new MemoryQueueBackend(0);
        // check no tasks in queue
        assertFalse(isTaskInQueue(WORKER_ACTOR_DEF, startTaskId, processId));

        // check not finished items in graph
        assertFalse(dependencyBackend.getGraph(processId).getNotFinishedItems().isEmpty());

        // recovery process
        new RecoveryTask(memoryQueueBackend, memoryQueueBackendStatistics, dependencyBackend, taskDao, backendBundle.getProcessBackend(), backendBundle.getTaskBackend(), 1l, processId).call();

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
        release(startTaskId, null, new Task[] {workerTask});

        // check worker task in queue
        assertTrue(isTaskPresent(workerTaskId, processId));

        // clean tasks from queues
        memoryQueueBackend = new MemoryQueueBackend(0);
        // check no tasks in queue
        assertFalse(isTaskInQueue(WORKER_ACTOR_DEF, startTaskId, processId));

        // reset not finished items
        dependencyBackend.changeGraph(new GraphDao.Updater() {
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

        // recovery process
        new RecoveryTask(memoryQueueBackend, memoryQueueBackendStatistics, dependencyBackend, taskDao, backendBundle.getProcessBackend(), backendBundle.getTaskBackend(), 1l, processId).call();

        // check tasks in queue
        assertTrue(isTaskInQueue(WORKER_ACTOR_DEF, workerTaskId, processId));
    }
}
