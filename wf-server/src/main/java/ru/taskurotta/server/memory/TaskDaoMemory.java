package ru.taskurotta.server.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.server.TaskDao;
import ru.taskurotta.server.model.TaskObject;
import ru.taskurotta.server.model.TaskStateObject;
import ru.taskurotta.server.transport.ArgContainer;
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 12:59 PM
 */
public class TaskDaoMemory implements TaskDao {


	private final static Logger log = LoggerFactory.getLogger(TaskDaoMemory.class);

	private static final String ACTOR_ID = "InMemoryActor";
	private static final int queueCapacity = 1000;
	private int pollDelay = 60;

	private Map<String, BlockingQueue<TaskObject>> queues = new ConcurrentHashMap<String, BlockingQueue<TaskObject>>();
	protected Map<UUID, TaskObject> taskMap = new ConcurrentHashMap<UUID, TaskObject>();
	private Map<UUID, AtomicInteger> atomicCountdownMap = new ConcurrentHashMap<UUID, AtomicInteger>();

	public TaskDaoMemory(int pollDelay) {
		this();

		this.pollDelay = pollDelay;
	}

	public TaskDaoMemory() {

		if (log.isTraceEnabled()) {

			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {

					while (true) {
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						log.trace("#### queues.size() = [{}] ", queues.size());

						for (String queueId : queues.keySet()) {
							BlockingQueue<TaskObject> queue = queues.get(queueId);
							log.trace("#### [{}] size = [{}]", queueId, queue.size());

						}

						log.trace("#### taskMap.size() = [{}]" + taskMap.size());
						for (UUID taskId : taskMap.keySet()) {
							log.trace("#### [{}]", taskMap.get(taskId));
						}

					}


				}
			});

			thread.start();
		}
	}


	/**
	 * @param taskId
	 * @return
	 */
	@Override
	public ArgContainer getTaskValue(UUID taskId) {
		TaskObject taskObj = taskMap.get(taskId);

		return taskObj.getValue();
	}


	@Override
	public void decrementCountdown(UUID taskId, int decrementValue) {

		AtomicInteger atomicCountdown = atomicCountdownMap.get(taskId);

		int taskCountdown = atomicCountdown.addAndGet(-decrementValue);
		log.debug("task [{}] countdown value is [{}] after decrement", taskId, taskCountdown);

		// ?? Is it really needed?
		TaskObject taskObj = taskMap.get(taskId);
		taskObj.setCountdown(taskCountdown);

		// push task to work (to queue)
		if (taskCountdown == 0) {
			addTaskToQueue(taskMap.get(taskId));
		}

	}

	public boolean registerExternalWaitFor(UUID taskId, UUID externalWaitForTaskId) {

		TaskObject taskObj = taskMap.get(externalWaitForTaskId);

		boolean result = false;

		// task state can be switched to done concurrently
		synchronized (taskObj) {
			if (!taskObj.getState().getValue().equals(TaskStateObject.STATE.done)) {
				List<UUID> waitingId = taskObj.getWaitingId();

				if (waitingId == null) {
					waitingId = new ArrayList<UUID>();
				}

				waitingId.add(taskId);
				taskObj.setWaitingId(waitingId);

				result = true;
			}
		}

		return result;

	}

	@Override
	public void logTaskResult(DecisionContainer taskResult) {
		// nothing to do, because memory implementation doesn't need recovery functionality
	}

	@Override
	public void unlogTaskResult(UUID taskId) {
		// nothing to do, because memory implementation doesn't need recovery functionality
	}

	@Override
	public void saveTaskValue(UUID taskId, ArgContainer value, TaskStateObject taskStateMemory) {

		TaskObject taskObj = taskMap.get(taskId);
		taskObj.setValue(value);

		// waitingId list can be modified concurrently for not finished (done) tasks
		synchronized (taskObj) {
			taskObj.setState(taskStateMemory);
		}
	}


	@Override
	public TaskObject findById(UUID taskId) {
		return taskMap.get(taskId);
	}

	@Override
	public void add(TaskObject taskObj) {
		taskMap.put(taskObj.getTaskId(), taskObj);

		int countdown = taskObj.getCountdown();

		if (countdown == 0) {
			// just put task to queue if it has no Promise.
			addTaskToQueue(taskObj);
		} else {
			AtomicInteger atomicCountdown = new AtomicInteger(countdown);
			atomicCountdownMap.put(taskObj.getTaskId(), atomicCountdown);
		}

	}

	@Override
	public TaskObject pull(ActorDefinition actorDefinition) {

		String queueId = getQueueName(actorDefinition.getName(), actorDefinition.getVersion());

		BlockingQueue<TaskObject> queue = getQueue(queueId);

		TaskObject task = null;
		try {
			task = queue.poll(pollDelay, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			// TODO: General policy about exceptions
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		if (task != null) {
			setProgressSate(task);
		}

		return task;
	}


	private void setProgressSate(TaskObject task) {

		TaskStateObject taskState = new TaskStateObject(ACTOR_ID, TaskStateObject.STATE.process, System.currentTimeMillis());
		task.setState(taskState);
	}


	protected String getQueueName(String actorDefinitionName, String actorDefinitionVersion) {
		return actorDefinitionName + '#' + actorDefinitionVersion;
	}

	private BlockingQueue<TaskObject> getQueue(String queueName) {

		BlockingQueue<TaskObject> queue = queues.get(queueName);
		if (queue == null) {
			synchronized (this) {
				queue = new ArrayBlockingQueue<TaskObject>(queueCapacity);
				queues.put(queueName, queue);
			}
		}

		return queue;
	}

	protected void addTaskToQueue(TaskObject taskMemory) {

		log.debug("addTaskToQueue taskId = [{}]", taskMemory.getTaskId());

		TaskTarget taskTarget = taskMemory.getTarget();

		String queueName = getQueueName(taskTarget.getName(), taskTarget.getVersion());
		Queue<TaskObject> queue = getQueue(queueName);
		queue.add(taskMemory);
	}
}
