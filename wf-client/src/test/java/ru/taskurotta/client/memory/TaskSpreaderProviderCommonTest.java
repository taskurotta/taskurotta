package ru.taskurotta.client.memory;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.server.TaskDao;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.TaskServerGeneral;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.server.memory.TaskDaoMemory;
import ru.taskurotta.server.model.TaskStateObject;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 13.02.13
 * Time: 17:04
 */
public class TaskSpreaderProviderCommonTest {

    private TaskDao taskDao;
    private TaskServer taskServer;
    private TaskSpreaderProviderCommon taskSpreaderProvider;
    private ObjectFactory objectFactory;

    @Decider(name = "testDecider")
    private static interface TestDecider {
    }

    @Worker(name = "testWorker")
    private static interface TestWorker {
    }

    private static final String DECIDER_NAME;
    private static final String DECIDER_VERSION;
    private static final String WORKER_NAME;
    private static final String WORKER_VERSION;

    static {
        ActorDefinition actorDefinition = ActorDefinition.valueOf(TestDecider.class);
        DECIDER_NAME = actorDefinition.getName();
        DECIDER_VERSION = actorDefinition.getVersion();

        actorDefinition = ActorDefinition.valueOf(TestWorker.class);
        WORKER_NAME = actorDefinition.getName();
        WORKER_VERSION = actorDefinition.getVersion();
    }

    @Before
    public void setUp() throws Exception {
        taskDao = new TaskDaoMemory();
        taskServer = new TaskServerGeneral(taskDao);
        taskSpreaderProvider = new TaskSpreaderProviderCommon(taskServer);
        objectFactory = new ObjectFactory();
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName, Object[] args) {
        TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
        Task task = new TaskImpl(id, taskTarget, args);
        return task;
    }

    public static Task workerTask(UUID id, TaskType type, String methodName, Object[] args) {
        TaskTarget taskTarget = new TaskTargetImpl(type, WORKER_NAME, WORKER_VERSION, methodName);
        Task task = new TaskImpl(id, taskTarget, args);
        return task;
    }

    public static Promise promise(Task task) {
        return Promise.createInstance(task.getId());
    }


    /**
     * - Put new task to queue
     * - Get it from queue
     * - release simple task result
     * - compare with original task
     *
     * @throws Exception
     */
    @Test
    public void testAddTask() {

        UUID taskId = UUID.randomUUID();
        Task deciderTask = deciderTask(taskId, TaskType.DECIDER_START, "start", null);
        taskServer.startProcess(objectFactory.dumpTask(deciderTask));

        TaskSpreader workerTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(TestDecider.class));

        // task should be in "wait" state
        assertEquals(TaskStateObject.STATE.wait, taskDao.findById(taskId).getState().getState());

        Task taskFromQueue = workerTaskSpreader.pull();

        // pulled task should be the same as added above
        assertEquals(taskId, taskFromQueue.getId());

        // task should be in "process" state
        assertEquals(TaskStateObject.STATE.process, taskDao.findById(taskId).getState().getState());

        TaskDecision taskDecision = new TaskDecisionImpl(taskId, null, null);
        workerTaskSpreader.release(taskDecision);

        // task should be in "done" state
        assertEquals(TaskStateObject.STATE.done, taskDao.findById(taskId).getState().getState());

    }


    /**
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
        Task taskA = deciderTask(taskIdA, TaskType.DECIDER_START, "start", null);

        // create taskB

        UUID taskIdB = UUID.randomUUID();
        Task taskB = deciderTask(taskIdB, TaskType.DECIDER_ASYNCHRONOUS, "asynchronousB", null);

        // create workerTaskA(promiseB)

        UUID workerTaskIdA = UUID.randomUUID();
        Task workerTaskA = workerTask(workerTaskIdA, TaskType.WORKER, "workA", new Object[]{promise(taskB)});

        // Add taskA to queue
        taskServer.startProcess(objectFactory.dumpTask(taskA));

        // pull task from queue
        // pulled task should be the same as added (as TaskDecision) above
        Task taskQueueA = deciderTaskSpreader.pull();
        assertEquals(taskIdA, taskQueueA.getId());

        // release taskA with return (promiseB) and task list (taskB, workerTaskA(promiseB)
        TaskDecision taskAResult = new TaskDecisionImpl(taskIdA, promise(taskB), new Task[]{taskB, workerTaskA});
        deciderTaskSpreader.release(taskAResult);

        // task A should be in "depend" state
        assertEquals(TaskStateObject.STATE.depend, taskDao.findById(taskIdA).getState().getState());

        // workTaskA should be in "wait" state
        assertEquals(TaskStateObject.STATE.wait, taskDao.findById(workerTaskIdA).getState().getState());
        assertEquals(1, taskDao.findById(workerTaskIdA).getCountdown());

        // pull task from queue
        // pulled task should be the same as added (as TaskDecision) above
        System.err.println("PULL TASK " + taskIdB);
        Task taskQueueB = deciderTaskSpreader.pull();
        assertEquals(taskIdB, taskQueueB.getId());

        // create taskC
        UUID taskIdC = UUID.randomUUID();
        Task taskC = deciderTask(taskIdC, TaskType.DECIDER_ASYNCHRONOUS, "asynchronousC", null);

        // release taskB with return (promiseC) and task list (taskC)
        TaskDecision taskBResult = new TaskDecisionImpl(taskIdB, promise(taskC), new Task[]{taskC});
        deciderTaskSpreader.release(taskBResult);

        // task B should be in "depend" state
        assertEquals(TaskStateObject.STATE.depend, taskDao.findById(taskIdB).getState().getState());

        // pull task from queue
        // pulled task should be the same as added (as TaskDecision) above
        Task taskQueueC = deciderTaskSpreader.pull();
        assertEquals(taskIdC, taskQueueC.getId());

        // release taskC with return (1) and task list ()
        TaskDecision taskCResult = new TaskDecisionImpl(taskIdC, Promise.asPromise(1), null);
        deciderTaskSpreader.release(taskCResult);

        // check all in done state
        // task A should be in "done" state
        assertEquals(TaskStateObject.STATE.done, taskDao.findById(taskIdA).getState().getState());
        // task B should be in "done" state
        assertEquals(TaskStateObject.STATE.done, taskDao.findById(taskIdB).getState().getState());
        // task C should be in "done" state
        assertEquals(TaskStateObject.STATE.done, taskDao.findById(taskIdC).getState().getState());
        // workTaskA should be in "wait" state
        assertEquals(TaskStateObject.STATE.wait, taskDao.findById(workerTaskIdA).getState().getState());
        assertEquals(0, taskDao.findById(workerTaskIdA).getCountdown());

        Task workerQueueTaskA = workerTaskSpreader.pull();

        assertEquals(TaskStateObject.STATE.process, taskDao.findById(workerTaskIdA).getState().getState());
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
//        assertEquals(taskDeciderStart, taskSpreader.pull());
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
//        assertEquals(taskDeciderStart, taskSpreader.pull());
//        assertEquals(taskDeciderAsync, taskSpreader.pull());
//
//        assertEquals(0, queue.size());
//    }
}
