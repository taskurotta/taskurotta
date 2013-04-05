package ru.taskurotta.backend.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskDefinition;
import ru.taskurotta.core.TaskType;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 9:34 PM
 */
public class MemoryTaskBackend implements TaskBackend {

    private final static Logger logger = LoggerFactory.getLogger(MemoryTaskBackend.class);

    private Map<UUID, TaskContainer> id2TaskMap = new ConcurrentHashMap<UUID, TaskContainer>();
    private Map<UUID, DecisionContainer> id2TaskDecisionMap = new ConcurrentHashMap<UUID, DecisionContainer>();
    private Map<TaskDefinition, Long> id2ProgressMap = new ConcurrentHashMap<TaskDefinition, Long>();
    
	//recovery process locks
//	private Map<UUID, Lock> locksMap = new ConcurrentHashMap<UUID, Lock>();
    
    @Override
    public void startProcess(TaskContainer taskContainer) {
        id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
    }

    @Override
    public TaskContainer getTaskToExecute(UUID taskId) {

        logger.debug("getTaskToExecute() taskId = [{}]", taskId);

        TaskContainer task = getTask(taskId);

        ArgContainer[] args = task.getArgs();

        if (args != null) {

            for (int i = 0; i < args.length; i++) {
                ArgContainer arg = args[i];
                if (arg.isPromise()) {
                    if (!TaskType.DECIDER_ASYNCHRONOUS.equals(task.getType())) {
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
        
        Long executionStarted = System.currentTimeMillis(); 
        TaskDefinition td = new TaskDefinition(taskId, task.getActorId(), task.getStartTime(), null, executionStarted); 
        id2ProgressMap.put(td, executionStarted);//Always create new entry due to Long executionStarted parameter

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

        removeProgressedTask(taskId);
        
        id2TaskDecisionMap.put(taskId, taskDecision);

        TaskContainer[] taskContainers = taskDecision.getTasks();
        if (taskContainers == null) {
            return;
        }

        for (TaskContainer taskContainer : taskContainers) {
            id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
        }
    }
    
    private void removeProgressedTask(UUID taskId) {
    	for(TaskDefinition td: id2ProgressMap.keySet()) {
    		if(taskId.equals(td.getTaskId())) {
    			id2ProgressMap.remove(td);
    			break;
    		}
    	}
    }
    
    @Override
    public void addDecisionCommit(UUID taskId) {
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
    	boolean result = false;
    	for(TaskDefinition td: id2ProgressMap.keySet()) {
    		if(taskId.equals(td.getTaskId())) {
    			result = true;
    			break;
    		}
    	}
    	return result;
    }

    public boolean isTaskReleased(UUID taskId) {
        return id2TaskDecisionMap.containsKey(taskId);
    }

// TODO: delete commented block
//	private static boolean hasTargetActorDefinition(ActorDefinition actorDefinition, TaskTarget taskTarget) {
//		return "default".equalsIgnoreCase(actorDefinition.getName()) 
//				|| (actorDefinition.getName().equals(taskTarget.getName()) && actorDefinition.getVersion().equals(taskTarget.getVersion()));
//	}
//
//	@Override
//	public boolean lockTask(UUID taskId, String lockerId, Date releaseDate) {
//		boolean result = false;
//		if(!isLocked(taskId)) {
//			locksMap.put(taskId, new Lock(lockerId, releaseDate));
//			result =  true;
//		}
//		
//		logger.debug("Locking task[{}] result is[{}]", taskId, result);
//		return result;
//	}
//	
//	@Override
//	public boolean unlockTask(UUID taskId, String lockerId) {
//		boolean result = true;
//		Lock lock = locksMap.get(taskId);
//		if(lock!=null) {
//			if(lockerId.equals(lock.getLockerId()) || lock.isExpired()) {
//				locksMap.remove(taskId);
//			} else {
//				logger.debug("LockerId[{}] cannot unlock task[{}], it has active lock till[{}] by [{}]", lockerId, taskId, lock.getExpires(), lock.getLockerId());
//				result = false;
//			}
//		}
//		logger.debug("Unlocking task[{}] result is[{}]", taskId, result);
//		return result;
//	}
//
//	private boolean isLocked(UUID taskId) {
//		boolean result =  locksMap.containsKey(taskId);
//		if(result) {
//			Lock lock = locksMap.get(taskId);
//			if(lock.isExpired()) {
//				result = !unlockTask(taskId, lock.getLockerId());
//			}
//		}
//		return result;
//	}

	@Override
	public List<TaskDefinition> getActiveTasks(String actorId, long timeFrom, long timeTill) {
		List<TaskDefinition> result = new ArrayList<TaskDefinition>();
		for(TaskDefinition taskDef: id2ProgressMap.keySet()) {
			Long taskAcceptedDate = id2ProgressMap.get(taskDef);
			if(taskAcceptedDate>timeFrom && taskAcceptedDate<timeTill) {
				result.add(taskDef);
			}
		}
		//logger.debug("Found[{}] active task for actorId[{}] in period from[{}] till[{}]", result.size(), actorId, timeFrom, timeTill);
		return result;
	}

	@Override
	public int resetActiveTasks(List<TaskDefinition> tasks) {
		int result = 0;
		if(tasks!=null && !tasks.isEmpty()) {
			for(TaskDefinition task: tasks) {
				if(id2ProgressMap.remove(task) != null) {
					result++;
				} else {
					logger.debug("Cannot reset task[{}]", task);
				}
			}
		}
		return result;
	}

}
