package ru.taskurotta.client.internal;

import org.junit.Before;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.service.MemoryServiceBundle;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.dependency.links.MemoryGraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.gc.MemoryGarbageCollectorService;
import ru.taskurotta.service.queue.MemoryQueueService;
import ru.taskurotta.service.recovery.GeneralRecoveryProcessService;
import ru.taskurotta.service.storage.BrokenProcessService;
import ru.taskurotta.service.storage.GeneralTaskService;
import ru.taskurotta.service.storage.MemoryBrokenProcessService;
import ru.taskurotta.service.storage.MemoryTaskDao;
import ru.taskurotta.service.storage.TaskDao;
import ru.taskurotta.test.TestTasks;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

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

    private static final UUID PASS = UUID.randomUUID();

    protected MemoryQueueService memoryQueueService;
    protected GeneralTaskService memoryStorageService;
    protected DependencyService dependencyService;
    protected GeneralRecoveryProcessService recoveryProcessService;
    protected BrokenProcessService brokenProcessService;
    protected ServiceBundle serviceBundle;
    protected GarbageCollectorService garbageCollectorService;
    protected GraphDao graphDao;

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
        serviceBundle = new MemoryServiceBundle(0, taskDao);
        memoryQueueService = (MemoryQueueService) serviceBundle.getQueueService();
        memoryStorageService = (GeneralTaskService) serviceBundle.getTaskService();
        dependencyService = serviceBundle.getDependencyService();
        brokenProcessService = new MemoryBrokenProcessService();
        graphDao = new MemoryGraphDao();
        garbageCollectorService = new MemoryGarbageCollectorService(serviceBundle.getProcessService(), graphDao, taskDao,
                                                                    1, 1000l);
        recoveryProcessService = new GeneralRecoveryProcessService(memoryQueueService, dependencyService,
                serviceBundle.getProcessService(), serviceBundle.getTaskService(), brokenProcessService, garbageCollectorService,
                1l, 1000l);

        taskServer = new GeneralTaskServer(serviceBundle);
        taskSpreaderProvider = new TaskSpreaderProviderCommon(taskServer);
        objectFactory = new ObjectFactory();
    }

    public boolean isTaskInProgress(UUID taskId, UUID processId) {
        return dependencyService.getGraph(processId).hasNotFinishedItem(taskId);
    }

    public void assertTaskInProgress(UUID taskId) {
        assertTrue(dependencyService.getGraph(processId).hasNotFinishedItem(taskId));
    }

    public boolean isTaskReleased(UUID taskId, UUID processId) {
        return memoryStorageService.isTaskReleased(taskId, processId);
    }

    public boolean isTaskPresent(UUID taskId, UUID processId) {
        return memoryStorageService.getTask(taskId, processId) != null;
    }


    public boolean isTaskInQueue(ActorDefinition actorDefinition, UUID taskId, UUID processId) {
        return memoryQueueService.isTaskInQueue(ActorUtils.getActorId(actorDefinition), actorDefinition.getTaskList(), taskId, processId);
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

    public static Task deciderTask(UUID id, TaskType type, String methodName, String[] failTypes, Object[] args, TaskOptions taskOptions) {
        TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, processId, taskTarget, args, taskOptions, failTypes);
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

    public Task pollWorkerTask() {
        TaskSpreader workerTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(AbstractTestStub.TestWorker.class));

        return workerTaskSpreader.poll();
    }

    public void release(UUID taskAId, Object value, Task... newTasks) {
        TaskDecision taskDecision = new TaskDecisionImpl(taskAId, processId, PASS, value, newTasks, 0l);
        release(taskDecision);
    }

    public void release(UUID taskAId, Throwable error) {
        TaskDecision taskDecision = new TaskDecisionImpl(taskAId, processId, PASS, error, null);
        release(taskDecision);
    }

    public void release(TaskDecision decision) {
        TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(AbstractTestStub.TestDecider.class));
        deciderTaskSpreader.release(decision);
    }

}
