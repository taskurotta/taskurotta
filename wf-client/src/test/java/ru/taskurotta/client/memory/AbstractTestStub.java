package ru.taskurotta.client.memory;

import org.junit.Before;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.server.TaskDao;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.TaskServerGeneral;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.server.memory.TaskDaoMemory;
import ru.taskurotta.test.TestTasks;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

/**
 * User: romario
 * Date: 3/29/13
 * Time: 3:34 PM
 */
public class AbstractTestStub {

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
        taskDao = new TaskDaoMemory(0);
        taskServer = new TaskServerGeneral(taskDao);
        taskSpreaderProvider = new TaskSpreaderProviderCommon(taskServer);
        objectFactory = new ObjectFactory();
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName, long startTime) {
        TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, taskTarget, startTime, 0, null, null);
        return task;
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName) {
        return deciderTask(id, type, methodName, null, null);
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName, Object[] args) {
        TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, taskTarget, args, null);
        return task;
    }

    public static Task deciderTask(UUID id, TaskType type, String methodName, Object[] args, TaskOptions taskOptions) {
        TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, taskTarget, args, taskOptions);
        return task;
    }

    public static Task workerTask(UUID id, TaskType type, String methodName, Object[] args) {
        TaskTarget taskTarget = new TaskTargetImpl(type, WORKER_NAME, WORKER_VERSION, methodName);
        Task task = TestTasks.newInstance(id, taskTarget, args);
        return task;
    }

    public static Promise promise(Task task) {
        return Promise.createInstance(task.getId());
    }

}
