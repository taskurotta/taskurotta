package ru.taskurotta.client.memory;

import java.util.UUID;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.core.ArgType;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskOptions;
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

/**
 * User: romario
 * Date: 3/26/13
 * Time: 2:07 PM
 */
public class NoWaitTest {

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
		taskDao = new TaskDaoMemory(0);
		taskServer = new TaskServerGeneral(taskDao);
		taskSpreaderProvider = new TaskSpreaderProviderCommon(taskServer);
		objectFactory = new ObjectFactory();
	}

	public static Task deciderTask(UUID id, TaskType type, String methodName) {
		return deciderTask(id, type, methodName, null, null);
	}

	public static Task deciderTask(UUID id, TaskType type, String methodName, Object[] args, TaskOptions taskOptions) {
		TaskTarget taskTarget = new TaskTargetImpl(type, DECIDER_NAME, DECIDER_VERSION, methodName);
		Task task = new TaskImpl(id, taskTarget, args, taskOptions);
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
	 * - A -> B, C, D(@NoWait B, C)
	 * - D -> E(B)
	 * <p/>
	 * D can be queued before task B has been processed
	 * E should be queued after task B has been processed
	 */
	@Test
	public void testAddTask() {

		UUID taskAId = UUID.randomUUID();
		Task deciderTaskA = deciderTask(taskAId, TaskType.DECIDER_START, "start");
		taskServer.startProcess(objectFactory.dumpTask(deciderTaskA));

		TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(TestDecider.class));

		Task taskAFromQueue = deciderTaskSpreader.pull();

		// should be task A
		assertEquals(taskAId, taskAFromQueue.getId());

		// task should be in "process" state
		assertEquals(TaskStateObject.STATE.process, taskDao.findById(taskAId).getState().getValue());

		// create B, C, D tasks
		UUID taskBId = UUID.randomUUID();
		Task deciderTaskB = deciderTask(taskBId, TaskType.DECIDER_START, "startB");

		UUID taskCId = UUID.randomUUID();
		Task deciderTaskC = deciderTask(taskCId, TaskType.DECIDER_START, "startC");

		UUID taskDId = UUID.randomUUID();
		Task deciderTaskD = deciderTask(taskDId, TaskType.DECIDER_START, "startD",
				new Object[]{promise(deciderTaskB), promise(deciderTaskC)},
				new TaskOptions(new ArgType[]{ArgType.NO_WAIT, ArgType.NONE}));

		TaskDecision taskADecision = new TaskDecisionImpl(taskAId, null, new Task[]{deciderTaskB, deciderTaskC, deciderTaskD});
		deciderTaskSpreader.release(taskADecision);

		// TODO: taskC and taskB may be pooled in different order

		Task taskCFromQueue = deciderTaskSpreader.pull();
		assertEquals(taskCId, taskCFromQueue.getId());

		Task taskBFromQueue = deciderTaskSpreader.pull();
		assertEquals(taskBId, taskBFromQueue.getId());

		Task taskDFromQueue = deciderTaskSpreader.pull();

		assertNull(taskDFromQueue);

		// release task C and task D should be queued

		TaskDecision taskCDecision = new TaskDecisionImpl(taskCId, null, null);
		deciderTaskSpreader.release(taskCDecision);

		taskDFromQueue = deciderTaskSpreader.pull();

		assertNotNull(taskDFromQueue);

		// release task D = create task E(B)

		UUID taskEId = UUID.randomUUID();
		Task deciderTaskE = deciderTask(taskEId, TaskType.DECIDER_START, "startE",
				new Object[]{promise(deciderTaskB)},
				null);

		TaskDecision taskDDecision = new TaskDecisionImpl(taskDId, null, new Task[]{deciderTaskE});
		deciderTaskSpreader.release(taskDDecision);

		// should be empty queue. we are still waiting task B

		Task taskEFromQueue = deciderTaskSpreader.pull();

		assertNull(taskEFromQueue);

		// release task B

		TaskDecision taskBDecision = new TaskDecisionImpl(taskBId, null, null);
		deciderTaskSpreader.release(taskBDecision);

		taskEFromQueue = deciderTaskSpreader.pull();

		// should be task E
		assertEquals(taskEId, taskEFromQueue.getId());

		// should be empty queue

		Task task = deciderTaskSpreader.pull();

		assertNull(task);
	}

}
