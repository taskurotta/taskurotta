package ru.taskurotta.client.memory;

import org.junit.Before;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.MemoryBackendBundle;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.process.MemoryBrokenProcessBackend;
import ru.taskurotta.backend.queue.MemoryQueueBackend;
import ru.taskurotta.backend.recovery.GeneralRecoveryProcessBackend;
import ru.taskurotta.backend.recovery.MemoryQueueBackendStatistics;
import ru.taskurotta.backend.storage.GeneralTaskBackend;
import ru.taskurotta.backend.storage.MemoryTaskDao;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.test.TestTasks;
import ru.taskurotta.transport.model.TaskType;
import ru.taskurotta.util.ActorDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * User: romario
 * Date: 3/29/13
 * Time: 3:34 PM
 */
public class AbstractTestStub {

    protected MemoryQueueBackend memoryQueueBackend;
    protected MemoryQueueBackendStatistics memoryQueueBackendStatistics;
    protected GeneralTaskBackend memoryStorageBackend;
    protected DependencyBackend dependencyBackend;
    protected GeneralRecoveryProcessBackend recoveryProcessBackend;
    protected BrokenProcessBackend brokenProcessBackend;
    protected BackendBundle backendBundle;

    protected TaskDao taskDao;

    protected TaskServer taskServer;
    protected TaskSpreaderProviderCommon taskSpreaderProvider;
    protected ObjectFactory objectFactory;

    @Decider(name = "testDecider")
    protected static interface TestDecider {
    }

    @Worker(name = "testWorker")
    protected static interface TestWorker {
    }

    protected static final String DECIDER_NAME;
    protected static final String DECIDER_VERSION;
    protected static final String WORKER_NAME;
    protected static final String WORKER_VERSION;

    protected static final ActorDefinition DECIDER_ACTOR_DEF;
    protected static final ActorDefinition WORKER_ACTOR_DEF;

    protected static final UUID processId = UUID.randomUUID();

    static {
        ActorDefinition actorDefinition = ActorDefinition.valueOf(TestDecider.class);
        DECIDER_NAME = actorDefinition.getName();
        DECIDER_VERSION = actorDefinition.getVersion();

        actorDefinition = ActorDefinition.valueOf(TestWorker.class);
        WORKER_NAME = actorDefinition.getName();
        WORKER_VERSION = actorDefinition.getVersion();

        DECIDER_ACTOR_DEF = ActorDefinition.valueOf(DECIDER_NAME, DECIDER_VERSION);
        WORKER_ACTOR_DEF = ActorDefinition.valueOf(WORKER_NAME, WORKER_VERSION);
    }

    @Before
    public void setUp() throws Exception {
        taskDao = new MemoryTaskDao();
        backendBundle = new MemoryBackendBundle(0, taskDao);
        memoryQueueBackend = (MemoryQueueBackend) backendBundle.getQueueBackend();
        memoryQueueBackendStatistics = new MemoryQueueBackendStatistics(memoryQueueBackend);
        memoryStorageBackend = (GeneralTaskBackend) backendBundle.getTaskBackend();
        dependencyBackend = backendBundle.getDependencyBackend();
        brokenProcessBackend = new MemoryBrokenProcessBackend();
        recoveryProcessBackend = new GeneralRecoveryProcessBackend(memoryQueueBackendStatistics, dependencyBackend, taskDao, backendBundle.getProcessBackend(), backendBundle.getTaskBackend(), brokenProcessBackend, 1l);

        taskServer = new GeneralTaskServer(backendBundle);
        taskSpreaderProvider = new TaskSpreaderProviderCommon(taskServer);
        objectFactory = new ObjectFactory();
    }

    public boolean isTaskInProgress(UUID taskId, UUID processId) {
        return dependencyBackend.getGraph(processId).hasNotFinishedItem(taskId);
    }

    public void assertTaskInProgress(UUID taskId) {
        assertTrue(dependencyBackend.getGraph(processId).hasNotFinishedItem(taskId));
    }

    public boolean isTaskReleased(UUID taskId, UUID processId) {
        return memoryStorageBackend.isTaskReleased(taskId, processId);
    }

    public boolean isTaskPresent(UUID taskId, UUID processId) {
        return null != memoryStorageBackend.getTask(taskId, processId);
    }


    public boolean isTaskInQueue(ActorDefinition actorDefinition, UUID taskId, UUID processId) {
        return memoryQueueBackend.isTaskInQueue(actorDefinition.getFullName(), actorDefinition.getTaskList(), taskId, processId);
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName, long startTime) {
        TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, processId, taskTarget, startTime, 0, null, null);
        return task;
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName) {
        return deciderTask(id, type, methodName, null, null);
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName, Object... args) {
        TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, processId, taskTarget, args, null);
        return task;
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName, Object[] args, TaskOptions taskOptions) {
        TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, processId, taskTarget, args, taskOptions);
        return task;
    }

    public static Task workerTask(UUID id, TaskType type, String methodName, Object[] args) {
        TaskTarget taskTarget = new TaskTargetImpl(type, WORKER_NAME, WORKER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, processId, taskTarget, args);
        return task;
    }

    public static Promise promise(Task task) {
        return Promise.createInstance(task.getId());
    }

    public void startProcess(Task task) {
        taskServer.startProcess(objectFactory.dumpTask(task));
    }

    public Task assertEmptyQueue() {
        TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(AbstractTestStub.TestDecider.class));

        Task polledTask = deciderTaskSpreader.poll();

        UUID polledTaskId = polledTask == null? null: polledTask.getId();

        assertEquals(null, polledTaskId);

        return polledTask;
    }

    public Task pollDeciderTask(UUID ... expectedTaskId) {
        TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(AbstractTestStub.TestDecider.class));

        Task polledTask = deciderTaskSpreader.poll();

        UUID polledTaskId = polledTask == null? null: polledTask.getId();

		List taskIdList = Arrays.asList(expectedTaskId);
		assertTrue(taskIdList.contains(polledTaskId));

        return polledTask;
    }

    public void release(UUID taskAId, Object value, Task... newTasks) {
        TaskDecision taskDecision = new TaskDecisionImpl(taskAId, processId, value, newTasks, 0l);

        TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(AbstractTestStub.TestDecider.class));
        deciderTaskSpreader.release(taskDecision);
    }
}
