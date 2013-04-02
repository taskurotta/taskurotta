package ru.taskurotta.client.memory;

import org.junit.Assert;
import org.junit.Test;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.backend.storage.model.TaskStateObject;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 13.02.13
 * Time: 17:04
 */
public class TaskSpreaderProviderCommonTest extends AbstractTestStub {

    /**
     * - Put new task to queue
     * - Get it from queue
     * - release simple task result
     * - compare with original task
     */
    @Test
    public void testAddTask() {

        UUID taskId = UUID.randomUUID();
        Task deciderTask = deciderTask(taskId, TaskType.DECIDER_START, "start", null);
        taskServer.startProcess(objectFactory.dumpTask(deciderTask));

        TaskSpreader workerTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(TestDecider.class));

        // task should be in queue
        assertTrue(isTaskInQueue(DECIDER_ACTOR_DEF, taskId));

        Task taskFromQueue = workerTaskSpreader.pull();

        // pulled task should be the same as added above
        assertEquals(taskId, taskFromQueue.getId());

        // task should be in "process" state
        assertTrue(isTaskInProgress(taskId));

        TaskDecision taskDecision = new TaskDecisionImpl(taskId, null, null);
        workerTaskSpreader.release(taskDecision);

        // task should be in "done" state
        Assert.assertFalse(isTaskInProgress(taskId));
        Assert.assertTrue(isTaskReleased(taskId));

    }


    /**
     * A
     * A -> B, wA(B)
     * - create taskA
     * - create taskB
     * - create workerTaskA(promiseB)
     * - release taskA with return (promiseB) and task list (taskB, workerTaskA(promiseB)
     * - create taskC
     * - release taskB with return (promiseC) and task list (taskC)
     * - release taskC with return (1) and task list ()
     * - check all in done state
     */
    @Test
    public void testABCResultDependency() {

        TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(TestDecider.class));
        TaskSpreader workerTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(TestWorker.class));

        // create taskA

        UUID taskIdA = UUID.randomUUID();
        System.err.println("taskIdA = " + taskIdA);
        Task taskA = deciderTask(taskIdA, TaskType.DECIDER_START, "taskA", null);

        // create taskB

        UUID taskIdB = UUID.randomUUID();
        Task taskB = deciderTask(taskIdB, TaskType.DECIDER_ASYNCHRONOUS, "taskB", null);

        // create workerTaskA(promiseB)

        UUID workerTaskIdA = UUID.randomUUID();
        Task workerTaskA = workerTask(workerTaskIdA, TaskType.WORKER, "workerTaskA", new Object[]{promise(taskB)});

        // Add taskA to queue
        taskServer.startProcess(objectFactory.dumpTask(taskA));

        // poll task from queue
        // pulled task should be the same as added (as TaskDecision) above
        Task taskQueueA = deciderTaskSpreader.pull();
        assertEquals(taskIdA, taskQueueA.getId());

        // release taskA with return (promiseB) and task list (taskB, workerTaskA(promiseB)
        TaskDecision taskAResult = new TaskDecisionImpl(taskIdA, promise(taskB), new Task[]{taskB, workerTaskA});
        deciderTaskSpreader.release(taskAResult);

        // task A should be in "depend" state
// TODO
//        assertEquals(TaskStateObject.STATE.depend, taskDao.findById(taskIdA).getState().getValue());

        // workTaskA should be in "wait" state
        Assert.assertTrue(isTaskWaitOtherTasks(workerTaskIdA, 1));

        // poll task from queue
        // pulled task should be the same as added (as TaskDecision) above
        Task taskQueueB = deciderTaskSpreader.pull();
        assertEquals(taskIdB, taskQueueB.getId());

        // create taskC
        UUID taskIdC = UUID.randomUUID();
        Task taskC = deciderTask(taskIdC, TaskType.DECIDER_ASYNCHRONOUS, "taskC", null);

        // release taskB with return (promiseC) and task list (taskC)
        TaskDecision taskBResult = new TaskDecisionImpl(taskIdB, promise(taskC), new Task[]{taskC});
        deciderTaskSpreader.release(taskBResult);

        // task B should be in "depend" state
// TODO
//        assertEquals(TaskStateObject.STATE.depend, taskDao.findById(taskIdB).getState().getValue());

        // poll task from queue
        // pulled task should be the same as added (as TaskDecision) above
        Task taskQueueC = deciderTaskSpreader.pull();
        assertEquals(taskIdC, taskQueueC.getId());

        // release taskC with return (1) and task list ()
        TaskDecision taskCResult = new TaskDecisionImpl(taskIdC, Promise.asPromise(1), null);
        deciderTaskSpreader.release(taskCResult);

        // check all in done state
        // task A should be in "done" state
        Assert.assertTrue(isTaskReleased(taskIdA));
//        assertEquals(TaskStateObject.STATE.done, taskDao.findById(taskIdA).getState().getValue());
        // task B should be in "done" state
        Assert.assertTrue(isTaskReleased(taskIdB));
//        assertEquals(TaskStateObject.STATE.done, taskDao.findById(taskIdB).getState().getValue());
        // task C should be in "done" state
        Assert.assertTrue(isTaskReleased(taskIdC));
//        assertEquals(TaskStateObject.STATE.done, taskDao.findById(taskIdC).getState().getValue());
        // workTaskA should be in "wait" state
        Assert.assertTrue(isTaskInQueue(WORKER_ACTOR_DEF, workerTaskIdA));

        Task workerQueueTaskA = workerTaskSpreader.pull();

        Assert.assertTrue(isTaskInProgress(workerTaskIdA));
//        assertEquals(TaskStateObject.STATE.process, taskDao.findById(workerTaskIdA).getState().getValue());
    }

    /**
     * Сценарии которые еще нужно протестировать (возможно объекдинить можно несколько тестов)
     * 4. вызов асинхронного метода координатора зависит от результата работы другого исполнителя и (или) координатора
     * 6. старт subworkflow (второго координатора нагляднее сделать) и получение результата от него
     */

//    @Test
//    public void testAddTask() throws Exception {
//        UUID uuidDeciderStart = UUID.randomUUID();
//        TaskTarget taskTargetDeciderStart = new TaskTargetImpl(TaskType.DECIDER_START, deciderClassName, deciderVersion, "start");
//        Task taskDeciderStart = new TaskImpl(uuidDeciderStart, taskTargetDeciderStart, null);
//        taskSpreaderProvider.addTask(taskDeciderStart, null, null);
//
//        UUID uuidDeciderAsync = UUID.randomUUID();
//        TaskTarget taskTargetDeciderAsync = new TaskTargetImpl(TaskType.DECIDER_ASYNCHRONOUS, deciderClassName, deciderVersion, "simpleAsync");
//        Task taskDeciderAsync = new TaskImpl(uuidDeciderAsync, taskTargetDeciderAsync, new Object[]{Promise.asPromise(0)});
//        taskSpreaderProvider.addTask(taskDeciderAsync, uuidDeciderStart, Collections.nCopies(1, uuidDeciderStart));
//
//        assertEquals(1, queue.size());
//        assertEquals(taskDeciderStart, queue.element());
//        assertEquals(taskDeciderStart, taskSpreader.poll());
//        assertEquals(0, queue.size());
//    }
//
//    @Test
//    public void testRelease() throws Exception {
//        UUID uuidDeciderStart = UUID.randomUUID();
//        TaskTarget taskTargetDeciderStart = new TaskTargetImpl(TaskType.DECIDER_START, deciderClassName, deciderVersion, "start");
//        Task taskDeciderStart = new TaskImpl(uuidDeciderStart, taskTargetDeciderStart, null);
//        taskSpreaderProvider.addTask(taskDeciderStart, null, null);
//
//        UUID uuidDeciderAsync = UUID.randomUUID();
//        TaskTarget taskTargetDeciderAsync = new TaskTargetImpl(TaskType.DECIDER_ASYNCHRONOUS, deciderClassName, deciderVersion, "simpleAsync");
//        Task taskDeciderAsync = new TaskImpl(uuidDeciderAsync, taskTargetDeciderAsync, new Object[]{Promise.asPromise(0)});
//        taskSpreaderProvider.addTask(taskDeciderAsync, uuidDeciderStart, Collections.nCopies(1, uuidDeciderStart));
//
//        TaskDecision taskResult = new TaskDecisionImpl(uuidDeciderAsync, Promise.asPromise(true), Collections.nCopies(1, taskDeciderAsync));
//        taskSpreaderProvider.release(taskResult);
//
//        assertEquals(2, queue.size());
//        assertTrue(queue.contains(taskDeciderStart));
//        assertTrue(queue.contains(taskDeciderAsync));
//
//        assertEquals(taskDeciderStart, taskSpreader.poll());
//        assertEquals(taskDeciderAsync, taskSpreader.poll());
//
//        assertEquals(0, queue.size());
//    }
}
