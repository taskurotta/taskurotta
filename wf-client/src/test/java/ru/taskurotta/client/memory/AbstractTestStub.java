package ru.taskurotta.client.memory;

import org.junit.Before;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.MemoryBackendBundle;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.MemoryGraphDao;
import ru.taskurotta.backend.queue.MemoryQueueBackend;
import ru.taskurotta.backend.storage.MemoryTaskBackend;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.test.TestTasks;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

/**
 * User: romario
 * Date: 3/29/13
 * Time: 3:34 PM
 */
public class AbstractTestStub {

    protected MemoryQueueBackend memoryQueueBackend;
    protected MemoryTaskBackend memoryStorageBackend;
    protected MemoryGraphDao memoryGraphDao;

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
//        taskDao = new TaskDaoMemory(0);
//        taskServer = new TaskServerGeneral(taskDao);
        BackendBundle backendBundle = new MemoryBackendBundle(0);
        memoryQueueBackend = (MemoryQueueBackend) backendBundle.getQueueBackend();
        memoryStorageBackend = (MemoryTaskBackend) backendBundle.getTaskBackend();
        memoryGraphDao = ((MemoryBackendBundle) backendBundle).getMemoryGraphDao();

        taskServer = new GeneralTaskServer(backendBundle);
        taskSpreaderProvider = new TaskSpreaderProviderCommon(taskServer);
        objectFactory = new ObjectFactory();
    }

    public boolean isTaskInProgress(UUID taskId) {
        return memoryStorageBackend.isTaskInProgress(taskId);
    }

    public boolean isTaskReleased(UUID taskId) {
        return memoryStorageBackend.isTaskReleased(taskId);
    }

    /**
     * @param taskId
     * @param taskQuantity -1 is any quantity
     * @return
     */
    public boolean isTaskWaitOtherTasks(UUID taskId, int taskQuantity) {

        Graph graph = memoryGraphDao.getGraph(processId);

        return graph.isTaskWaitOtherTasks(taskId, taskQuantity);

    }

    public boolean isTaskInQueue(ActorDefinition actorDefinition, UUID taskId) {
        return memoryQueueBackend.isTaskInQueue(actorDefinition, taskId);
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName, long startTime) {
        TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, processId, taskTarget, startTime, 0, null, null);
        return task;
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName) {
        return deciderTask(id, type, methodName, null, null);
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName, Object[] args) {
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

}
