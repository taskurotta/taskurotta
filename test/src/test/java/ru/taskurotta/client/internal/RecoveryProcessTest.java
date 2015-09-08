package ru.taskurotta.client.internal;

import org.junit.Test;
import ru.taskurotta.core.Task;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
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
        assertNotNull(queueService.poll(ActorUtils.getActorId(DECIDER_ACTOR_DEF), DECIDER_ACTOR_DEF.getTaskList()));

        // clean tasks and graph
        queueService = serviceBundle.newQueueService();
        graphDao.deleteGraph(processId);

        // check no tasks in queue
        assertNull(queueService.poll(ActorUtils.getActorId(DECIDER_ACTOR_DEF), DECIDER_ACTOR_DEF.getTaskList()));

        // check no graph for process
        assertNull(dependencyService.getGraph(processId));

        // recovery process
        recoveryProcessService.resurrectProcess(processId);

        // check not null graph for process
        assertNotNull(dependencyService.getGraph(processId));

        // check start task in queue
        assertNotNull(queueService.poll(ActorUtils.getActorId(DECIDER_ACTOR_DEF), DECIDER_ACTOR_DEF.getTaskList()));
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
        queueService = serviceBundle.newQueueService();
        recoveryProcessService.setQueueService(queueService);

        // check no tasks in queue
        assertNull(queueService.poll(ActorUtils.getActorId(WORKER_ACTOR_DEF), WORKER_ACTOR_DEF.getTaskList()));

        // check not finished items in graph
        assertFalse(dependencyService.getGraph(processId).getNotFinishedItems().isEmpty());

        // update lastEnqueueTime for recovery
        queueService.poll(ActorUtils.getActorId(WORKER_ACTOR_DEF), WORKER_ACTOR_DEF.getTaskList());

        // recovery process
        recoveryProcessService.resurrectProcess(processId);

        // check tasks in queue
        assertNotNull(queueService.poll(ActorUtils.getActorId(WORKER_ACTOR_DEF), WORKER_ACTOR_DEF.getTaskList()));
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
        queueService = serviceBundle.newQueueService();

        // check no tasks in queue
        assertNull(queueService.poll(ActorUtils.getActorId(WORKER_ACTOR_DEF), WORKER_ACTOR_DEF.getTaskList()));

        // reset not finished items
        dependencyService.changeGraph(new GraphDao.Updater() {
            @Override
            public UUID getProcessId() {
                return processId;
            }

            @Override
            public boolean apply(Graph graph) {
                graph.setVersion(graph.getVersion() + 1);
                graph.setNotFinishedItems(new HashMap<>());
                return true;
            }
        });

        queueService.poll(ActorUtils.getActorId(WORKER_ACTOR_DEF), WORKER_ACTOR_DEF.getTaskList());

        // recovery process
        recoveryProcessService.resurrectProcess(processId);

        // check tasks in queue
        assertNull(queueService.poll(ActorUtils.getActorId(WORKER_ACTOR_DEF), WORKER_ACTOR_DEF.getTaskList()));
    }
}
