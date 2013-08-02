package ru.taskurotta.client.memory;

import org.junit.Before;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.MemoryBackendBundle;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.graph.GraphDependencyBackend;
import ru.taskurotta.backend.dependency.graph.TaskNode;
import ru.taskurotta.backend.dependency.graph.TaskNodeDao;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.MemoryGraphDao;
import ru.taskurotta.backend.queue.MemoryQueueBackend;
import ru.taskurotta.backend.storage.GeneralTaskBackend;
import ru.taskurotta.backend.storage.MemoryTaskDao;
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

import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * User: romario
 * Date: 3/29/13
 * Time: 3:34 PM
 */
public class AbstractTestStub {

    protected MemoryQueueBackend memoryQueueBackend;
    protected GeneralTaskBackend memoryStorageBackend;
    protected DependencyBackend dependencyBackend;
    protected BackendBundle backendBundle;

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
        backendBundle = new MemoryBackendBundle(0, new MemoryTaskDao());
        memoryQueueBackend = (MemoryQueueBackend) backendBundle.getQueueBackend();
        memoryStorageBackend = (GeneralTaskBackend) backendBundle.getTaskBackend();
        dependencyBackend = backendBundle.getDependencyBackend();
        //memoryGraphDao = ((MemoryBackendBundle) backendBundle).getMemoryGraphDao();

        taskServer = new GeneralTaskServer(backendBundle);
        taskSpreaderProvider = new TaskSpreaderProviderCommon(taskServer);
        objectFactory = new ObjectFactory();
    }

    public boolean isTaskInProgress(UUID taskId, UUID processId) {
        return dependencyBackend.getGraph(processId).hasNotFinishedItem(taskId);
    }

    public boolean isTaskReleased(UUID taskId, UUID processId) {
        return memoryStorageBackend.isTaskReleased(taskId, processId);
    }

    public boolean isTaskPresent(UUID taskId, UUID processId) {
        return null != memoryStorageBackend.getTask(taskId, processId);
    }

    /**
     * @param taskId
     * @param taskQuantity -1 is any quantity
     * @return
     */
    public boolean isTaskWaitOtherTasks(UUID taskId, int taskQuantity) {

        if (dependencyBackend instanceof GraphDependencyBackend) {
            TaskNodeDao tnd = ((GraphDependencyBackend) dependencyBackend).getTaskNodeDao();
            TaskNode taskNode = tnd.getNode(taskId, processId);
            return !taskNode.isReleased();
        } else{
            MemoryGraphDao memoryGraphDao = ((MemoryBackendBundle) backendBundle).getMemoryGraphDao();
            Graph graph = memoryGraphDao.getGraph(processId);

            return graph != null && graph.isTaskWaitOtherTasks(taskId, taskQuantity);

        }

    }

    public boolean isTaskInQueue(ActorDefinition actorDefinition, UUID taskId, UUID processId) {
        return memoryQueueBackend.isTaskInQueue(actorDefinition, taskId, processId);
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

    public void startProcess(Task task) {
        taskServer.startProcess(objectFactory.dumpTask(task));
    }

    public Task pollDeciderTask(UUID expectedTaskId) {
        TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(AbstractTestStub.TestDecider.class));

        Task polledTask = deciderTaskSpreader.poll();

        UUID polledTaskId = polledTask == null? null: polledTask.getId();

        assertEquals(expectedTaskId, polledTaskId);

        return polledTask;
    }

    public void release(UUID taskAId, Object value, Task[] newTasks) {
        TaskDecision taskADecision = new TaskDecisionImpl(taskAId, processId, value, newTasks);

        TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(AbstractTestStub.TestDecider.class));
        deciderTaskSpreader.release(taskADecision);
    }
}
