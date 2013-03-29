package ru.taskurotta.server;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.core.ArgType;
import ru.taskurotta.server.config.ServerConfig;
import ru.taskurotta.server.config.ServerConfigAware;
import ru.taskurotta.server.model.TaskObject;
import ru.taskurotta.server.model.TaskStateObject;
import ru.taskurotta.server.service.ExpiredTaskProcessorService;
import ru.taskurotta.server.transport.ArgContainer;
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.server.transport.TaskOptionsContainer;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 1:09 PM
 */
public class TaskServerGeneral implements TaskServer, ServerConfigAware {

    private final static Logger log = LoggerFactory.getLogger(TaskServerGeneral.class);

    private static final String ACTOR_ID = "InMemoryActor";

    private TaskDao taskDao;
    
    private ServerConfig serverConfig;
    
    private String expirationCheckSchedule;
    
	public TaskServerGeneral(TaskDao taskDao) {

        this.taskDao = taskDao;
    }

    @Override
    public void startProcess(TaskContainer task) {
        addTask(task, null, null, null, false);
    }

    @Override
    public TaskContainer pull(ActorDefinition actorDefinition) {
        return pullInternal(actorDefinition);
    }

    @Override
    public void release(DecisionContainer taskResult) {
        releaseInternal(taskResult);
    }

    private TaskContainer pullInternal(ActorDefinition actorDefinition) {

        TaskObject task = taskDao.pull(actorDefinition);

        if (task == null) {
            return null;
        }

        ArgContainer[] args = task.getArgs();

        if (args != null) {

            for (ArgContainer arg : args) {
                if (arg.isPromise() && arg.getJSONValue() == null) {
                    ArgContainer value = taskDao.getTaskValue((arg).getTaskId());

                    // resolved Promise. value may be null for NoWait promises
                    if (value != null) {
                        arg.setJSONValue(value.getJSONValue());
                        arg.setClassName(value.getClassName());
                        arg.setReady(true);
                    }

                }
            }

        }

        return task;
    }

    protected TaskObject getTaskById(UUID taskId) {
        return taskDao.findById(taskId);
    }


    public void addTask(TaskContainer task, UUID parentTaskId, List<UUID> waitingId, Set<UUID> externalWaitForTasks,
                        boolean isDependTask) {

        log.debug("Add task = [{}]", task);

        TaskObject taskObj = new TaskObject(task);
        UUID taskId = taskObj.getTaskId();

        taskObj.setParentId(parentTaskId);
        taskObj.setWaitingId(waitingId);
        taskObj.setDependTask(isDependTask);

        log.debug("Add task [{}]. waitingId = [{}]", taskId, waitingId);

        TaskStateObject taskStateObj = new TaskStateObject(ACTOR_ID, TaskStateObject.STATE.wait, System.currentTimeMillis());
        taskObj.setState(taskStateObj);


        int countdown = 0;

        ArgContainer[] argContainers = task.getArgs();

        if (argContainers != null) {

            TaskOptionsContainer taskOptionsContainer = task.getOptions();
            ArgType[] argTypes = null;

            if (taskOptionsContainer != null) {
                argTypes = taskOptionsContainer.getArgTypes();
            }


            for (int i = 0; i < argContainers.length; i++) {

                // skip NoWait
                if (argTypes != null && argTypes[i].equals(ArgType.NO_WAIT)) {
                    continue;
                }

                ArgContainer argContainer = argContainers[i];

                if (argContainer != null && argContainer.isPromise() && !argContainer.isReady()) {
                    countdown++;
                }
            }
        }

        log.debug("Add task id [{}]. countdown = {}", taskId, countdown);

        if (countdown != 0) {
            taskObj.setCountdown(countdown);
        }

        taskDao.add(taskObj);

        // TODO: register task id in external tasks and decrement countdown if not success
        if (externalWaitForTasks != null) {

            int decrementValue = 0;

            for (UUID externalWaitForTaskId: externalWaitForTasks) {
                if (!taskDao.registerExternalWaitFor(taskId, externalWaitForTaskId)) {
                    decrementValue ++;
                }
            }

            if (decrementValue > 0) {
                taskDao.decrementCountdown(taskId, decrementValue);
            }
        }

    }

    private void releaseInternal(DecisionContainer taskResult) {

        taskDao.logTaskResult(taskResult);

        processReleasedTask(taskResult);

        taskDao.unlogTaskResult(taskResult.getTaskId());
    }


    private void processReleasedTask(DecisionContainer taskResult) {


        // 1. Изменить state задачи через Dao
        // 2. Подабовлять все задачи в хранилище через Dao

        // set to Done\Depend state first! Task can be in Depend state and its dependent task can finish before.

        UUID taskId = taskResult.getTaskId();
        ArgContainer value = taskResult.getValue();

        UUID dependTaskId = null;
        TaskStateObject.STATE currentState = TaskStateObject.STATE.done;

        log.debug("processReleasedTask taskId = [{}]", taskId);

        // calculate new state
        if (value != null && value.isPromise()) {

            log.debug("Task id [{}] has promise isReady = {} state", value.getTaskId(), value.isReady());

            if (!value.isReady()) {
                currentState = TaskStateObject.STATE.depend;
                dependTaskId = value.getTaskId();
            }
        }

        TaskStateObject taskStateObj = new TaskStateObject(ACTOR_ID, currentState, System.currentTimeMillis());

        taskDao.saveTaskValue(taskId, value, taskStateObj);


        // - registration of all new tasks
        TaskContainer[] childTasks = taskResult.getTasks();
        if (childTasks != null) {

            // iterate in reverse order cause independent task can be released too early.
            for (int i = childTasks.length - 1; i >= 0; i--) {

                TaskContainer childTask = childTasks[i];
                UUID childTaskId = childTask.getTaskId();

                List<UUID> waitingId = null;

                // go throw all tasks and find Promise argument with childTaskId
                for (TaskContainer otherChildTask : childTasks) {

                    // skip the same task
                    if (otherChildTask.getTaskId().equals(childTaskId)) {
                        continue;
                    }

                    ArgContainer args[] = otherChildTask.getArgs();

                    TaskOptionsContainer taskOptionsContainer = otherChildTask.getOptions();
                    ArgType[] argTypes = taskOptionsContainer != null ? taskOptionsContainer.getArgTypes() : null;

                    if (args != null) {
                        for (int j = 0; j < args.length; j++) {
                            ArgContainer arg = args[j];

                            boolean isPromise = arg.isPromise();

                            // skip not promises or resolved promises
                            if (!isPromise || (isPromise && arg.isReady())) {
                                continue;
                            }

                            // skip @NoWait promises
                            if (argTypes != null) {
                                if (argTypes[j].equals(ArgType.NO_WAIT)) {
                                    continue;
                                }
                            }

                            UUID otherChildTaskTaskId = otherChildTask.getTaskId();

                            if (arg.getTaskId().equals(childTaskId)) {
                                if (waitingId == null) {
                                    waitingId = new ArrayList<UUID>();
                                }

                                waitingId.add(otherChildTaskTaskId);
                            }

                            // @Wait stuff
//							if (isPromiseCollection()) {
//								for (Object obj : (Collection)arg.getObject())
//							}
                        }
                    }
                }


                // find external dependencies
                Set externalWaitForTasks = null;
                ArgContainer args[] = childTask.getArgs();

                if (args != null) {

                    Set internalDepsIds = null;

                    for (ArgContainer arg : args) {

                        boolean isPromise = arg.isPromise();

                        // skip not promises or resolved promises
                        if (!isPromise || (isPromise && arg.isReady())) {
                            continue;
                        }

                        // lazy initialization of Set
                        if (internalDepsIds == null) {

                            // create hash set of internal dependencies (list of task id)
                            internalDepsIds = new HashSet(childTasks.length);
                            for (TaskContainer otherChildTask : childTasks) {
                                internalDepsIds.add(otherChildTask.getTaskId());
                            }

                        }

                        // Is it external promise?
                        if (!internalDepsIds.contains(arg.getTaskId())) {
                            if (externalWaitForTasks == null) {
                                externalWaitForTasks = new HashSet();
                            }

                            externalWaitForTasks.add(arg.getTaskId());
                        }

                    }

                }


                addTask(childTask, taskId, waitingId, externalWaitForTasks,
                        dependTaskId != null && dependTaskId.equals(childTaskId));
            }
        }

        // recursion start compileDoneTask(taskMemory)
        if (currentState == TaskStateObject.STATE.done) {

            TaskObject taskObj = taskDao.findById(taskId);
            resolveDependency(taskObj);
        }

    }


    private void resolveDependency(TaskObject taskObj) {

        log.debug("resolveDependency taskId = [{}]", taskObj.getTaskId());

        // analise all waiting tasks
        // - decrement task.countdown for all tasks in taskMemory.waitingId list
        List<UUID> waitingUUIDs = taskObj.getWaitingId();
        if (waitingUUIDs != null && !waitingUUIDs.isEmpty()) {
            for (UUID uuid : waitingUUIDs) {

                log.debug("task for countdown decrement [{}]", uuid);

                taskDao.decrementCountdown(uuid, 1);

            }
        }

        // analise parent and its "depend" state
        if (taskObj.isDependTask()) {

            log.debug("task has parent task [{}]", taskObj.getParentId());

            TaskObject parentTask = taskDao.findById(taskObj.getParentId());

            log.debug("parent task state is [{}]", parentTask.getState());

            ArgContainer taskValue = taskObj.getValue();

            // - prepare new state
            TaskStateObject taskStateObj = new TaskStateObject(ACTOR_ID, TaskStateObject.STATE.done, System.currentTimeMillis());


            taskDao.saveTaskValue(taskObj.getParentId(), taskValue, taskStateObj);

            // decrement countdown on waiting tasks
            // check its parent and its "depend" state"
            // set to done state
            //compileDoneTask(taskMemory)
            resolveDependency(parentTask);
        }

    }
    
    @PostConstruct
    public void runExpiredTaskScheduler() throws ParseException {
		if(expirationCheckSchedule!=null && expirationCheckSchedule.trim().length()>0) {
			ExpiredTaskProcessorService service = new ExpiredTaskProcessorService();
			service.setServerConfig(serverConfig);
			service.setTaskDao(taskDao);
			service.setSchedule(expirationCheckSchedule);
			
			Thread runner = new Thread(service);
			runner.setDaemon(true);
			runner.start();
	    }
	}

	public void setServerConfig(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}
	
	public void setExpirationCheckSchedule(String expirationCheckSchedule) {
		this.expirationCheckSchedule = expirationCheckSchedule;
	}


}
