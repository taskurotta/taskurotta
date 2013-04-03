package ru.taskurotta.backend.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.Lock;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 9:34 PM
 */
public class MemoryStorageBackend implements StorageBackend {

	private final static Logger logger = LoggerFactory.getLogger(MemoryStorageBackend.class);

	private Map<UUID, TaskContainer> id2TaskMap = new ConcurrentHashMap<UUID, TaskContainer>();
	private Map<UUID, DecisionContainer> id2TaskDecisionMap = new ConcurrentHashMap<UUID, DecisionContainer>();
	//key: UUID, value: Date = enqueue date
	private Map<UUID, Date> id2ProgressMap = new ConcurrentHashMap<UUID, Date>();

	//recovery process locks
	private Map<UUID, Lock> locksMap = new ConcurrentHashMap<UUID, Lock>();

	@Override
	public void addProcess(TaskContainer taskContainer) {
		id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
	}

	@Override
	public TaskContainer getTaskToExecute(UUID taskId) {

		logger.debug("getTaskToExecute() taskId = [{}]", taskId);
		if(isLocked(taskId)) {
			return null;
		}
		
		TaskContainer task = getTask(taskId);


		ArgContainer[] args = task.getArgs();

		if (args != null) {

			for (int i = 0; i < args.length; i++) {
				ArgContainer arg = args[i];
				if (arg.isPromise()) {
					if (!TaskType.DECIDER_ASYNCHRONOUS.equals(task.getTarget().getType())) {
						ArgContainer value = getTaskValue(arg.getTaskId());
						args[i] = value;
					} else {
						if (arg.getJSONValue() == null) {
							// resolved Promise. value may be null for NoWait promises

							ArgContainer value = getTaskValue(arg.getTaskId());
									if (value != null) {
										arg.setJSONValue(value.getJSONValue());
										arg.setClassName(value.getClassName());
										arg.setReady(true);
									}
						}
					}
				}
			}

		}

		id2ProgressMap.put(taskId, new Date());

		return task;
	}


	private ArgContainer getTaskValue(UUID taskId) {

		logger.debug("getTaskValue() taskId = [{}]", taskId);

		DecisionContainer taskDecision = id2TaskDecisionMap.get(taskId);

		if (taskDecision == null) {
			return null;
		}

		ArgContainer argContainer = taskDecision.getValue();

		if (argContainer == null) {
			return null;
		}

		if (!argContainer.isPromise()) {
			return argContainer;
		}

		if (argContainer.isPromise() && !argContainer.isReady()) {
			return getTaskValue(argContainer.getTaskId());
		}

		return argContainer;
	}


	@Override
	public TaskContainer getTask(UUID taskId) {
		return id2TaskMap.get(taskId);
	}

	@Override
	public void addError(UUID taskId, ErrorContainer asyncTaskError, boolean shouldBeRestarted) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void addDecision(DecisionContainer taskDecision) {

		logger.debug("addDecision() taskDecision = [{}]", taskDecision);

		UUID taskId = taskDecision.getTaskId();

		id2ProgressMap.remove(taskId);
		id2TaskDecisionMap.put(taskId, taskDecision);

		TaskContainer[] taskContainers = taskDecision.getTasks();
		if (taskContainers == null) {
			return;
		}

		for (TaskContainer taskContainer : taskContainers) {
			id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
		}
	}

	@Override
	public void addDecisionCommit(UUID taskId, boolean processFinished) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void addErrorCommit(UUID taskId) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<TaskContainer> getAllRunProcesses() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<DecisionContainer> getAllTaskDecisions(UUID processId) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public boolean isTaskInProgress(UUID taskId) {
		return id2ProgressMap.containsKey(taskId);
	}

	public boolean isTaskReleased(UUID taskId) {
		return id2TaskDecisionMap.containsKey(taskId);
	}

	@Override
	public List<TaskContainer> getExpiredTasks(ActorDefinition actorDefinition, long timeout, int limit) {
		List<TaskContainer> result = new ArrayList<TaskContainer>();
		int limitCounter = 0;
		for(UUID uuid: id2ProgressMap.keySet()) {
			if((limit>0) && limitCounter >= limit) {
				break;
			} else if(isLocked(uuid)) {
				continue;
			} else {
				Date expirationDate = new Date(id2ProgressMap.get(uuid).getTime() + timeout);
				if(expirationDate.before(new Date())) {
					TaskContainer task = getTask(uuid);
					if(hasTargetActorDefinition(actorDefinition, task.getTarget())) {
						result.add(task);
						limitCounter++;
					}
				}
			}
		}

		return result;
	}

	private static boolean hasTargetActorDefinition(ActorDefinition actorDefinition, TaskTarget taskTarget) {
		return "default".equalsIgnoreCase(actorDefinition.getName()) 
				|| (actorDefinition.getName().equals(taskTarget.getName()) && actorDefinition.getVersion().equals(taskTarget.getVersion()));
	}

	@Override
	public boolean lockTask(UUID taskId, String lockerId, Date releaseDate) {
		if(!isLocked(taskId)) {
			locksMap.put(taskId, new Lock(lockerId, releaseDate));
			return true;
		} else {
			return false;	
		}
	}
	
	@Override
	public boolean unlockTask(UUID taskId, String lockerId) {
		boolean result = true;
		Lock lock = locksMap.get(taskId);
		if(lock!=null) {
			if(lockerId.equals(lock.getLockerId()) || lock.isExpired()) {
				locksMap.remove(taskId);
			} else {
				logger.debug("LockerId[{}] cannot unlock task[{}], it has active lock till[{}] by [{}]", lockerId, taskId, lock.getExpires(), lock.getLockerId());
				result = false;
			}
		}
		
		return result;
	}

	private boolean isLocked(UUID taskId) {
		boolean result =  locksMap.containsKey(taskId);
		if(result) {
			Lock lock = locksMap.get(taskId);
			if(lock.isExpired()) {
				result = !unlockTask(taskId, lock.getLockerId());
			}
		}
		return result;
	}

	@Override
	public void resetTask(UUID taskId) {
		id2ProgressMap.remove(taskId);
	}
		
	
}
