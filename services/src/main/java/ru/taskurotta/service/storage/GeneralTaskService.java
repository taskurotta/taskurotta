package ru.taskurotta.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.TaskInfoRetriever;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.Decision;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 9:34 PM
 */
public class GeneralTaskService implements TaskService, TaskInfoRetriever {

    private final static Logger logger = LoggerFactory.getLogger(GeneralTaskService.class);

    private TaskDao taskDao;
    private long workerTimeoutMilliseconds;

    public GeneralTaskService(TaskDao taskDao, long workerTimeoutMilliseconds) {
        this.taskDao = taskDao;
        this.workerTimeoutMilliseconds = workerTimeoutMilliseconds;
    }

    @Override
    public void startProcess(TaskContainer taskContainer) {
        taskDao.addTask(taskContainer);
    }

    @Override
    public TaskContainer getTaskToExecute(UUID taskId, UUID processId, boolean simulate) {
        logger.debug("getTaskToExecute(taskId[{}], processId[{}]) started", taskId, processId);

        TaskContainer task = getTask(taskId, processId);

        // WARNING: "task" object is the same instance as In memory data storage. So we should use  it deep clone
        // due guarantees for its immutability.

        if (task == null) {
            logger.warn("Inconsistent state, taskId[{}] does not present at task service", taskId);
            return null;
        }

        ArgContainer[] args = task.getArgs();

        if (args != null) {//updating ready Promises args into real values

            if (logger.isDebugEnabled()) {
                logger.debug("Task id[{}], type[{}], method[{}] args before swap processing [{}]", task.getTaskId(), task.getType(), task.getMethod(), Arrays.asList(args));
            }

            TaskOptionsContainer taskOptionsContainer = task.getOptions();
            ArgType[] argTypes = null;

            if (taskOptionsContainer != null) {
                argTypes = task.getOptions().getArgTypes();
            }

            for (int i = 0; i < args.length; i++) {
                ArgContainer arg = args[i];

                if (arg == null) {
                    continue;
                }

                ArgType argType = argTypes == null ? null : argTypes[i];

                // don't try to calculate @NoWait promise.
                // there are so many cases when it is really needed
                // and we can introduce new annotation to mark that promices
                // with new type, for example: NO_WAIT_TRY.
                if (argType == ArgType.NO_WAIT) {
                    continue;
                }

                if (arg.isPromise()) {
                    args[i] = processPromiseArgValue(arg, processId, task, argType);
                } else if (arg.isCollection()) {//can be collection of promises, case should be checked
                    ArgContainer[] compositeValue = arg.getCompositeValue();

                    if (compositeValue != null && compositeValue.length > 0) {
                        for (int j = 0; j < compositeValue.length; j++) {
                            ArgContainer innerArg = compositeValue[j];
                            if (innerArg.isPromise()) {
                                compositeValue[j] = processPromiseArgValue(innerArg, processId, task, argType);
                            }
                        }
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Task id[{}], type[{}], method[{}] args after swap processing [{}]", task.getTaskId(), task.getType(), task.getMethod(), Arrays.asList(args));
            }
        }

        // todo: timeout should be calculated for every actor

        if (!simulate) {
            // todo: actors should support acceptLast and acceptFirst strategies depends of their needs
            Decision decision = taskDao.startTask(taskId, processId, workerTimeoutMilliseconds, false);

            if (decision == null) {
                logger.debug("{}/{} Task can not be executed. It is already started or finished", taskId, processId);

                return null;
            }

            task.setErrorAttempts(decision.getErrorAttempts());

//            task.setPass(pass);
        }

        return task;
    }


    private ArgContainer processPromiseArgValue(ArgContainer pArg, UUID processId, TaskContainer task, ArgType argType) {

        // Only decider asynchronous methods can use Promise args, NOT start method
        boolean isDeciderAsynchronousTaskType = isDeciderAsynchronousTaskType(task.getType());

        if (pArg.isReady()) {    // Promise.asPromise() was used as an argument, so there is no taskValue, it is simply Promise wrapper for a worker
            logger.debug("Got initialized promise, switch it to value");
            return isDeciderAsynchronousTaskType ? pArg : pArg.updateType(false);              // simply strip of promise wrapper
        }

        UUID taskId = pArg.getTaskId();

        //try to find promise value obtained by its task result
        ArgContainer taskValue = null;

        try {
            taskValue = getTaskValue(taskId, processId, ArgType.NO_WAIT.equals(argType));
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Not initialized promise before execute [" + task + "]", e);
        }

        // Task not completed yet and not initialized Promise is acceptable by Decider
        if (taskValue == null) {
            return pArg;
        }

        ArgContainer newArg = new ArgContainer(taskValue);
        if (isDeciderAsynchronousTaskType) {

            // set real value into promise for Decider tasks
            newArg.setPromise(true);
            newArg.setTaskId(taskId);
        } else {

            // swap promise with real value for Actor tasks
            newArg.setPromise(false);
        }

        return newArg;
    }

    private static boolean isDeciderAsynchronousTaskType(TaskType taskType) {
        return TaskType.DECIDER_ASYNCHRONOUS.equals(taskType);
    }

    /**
     * @return ArgContainer (even in case of null value result), null if it acceptable (isNoWait is true)
     * or throw IllegalArgumentException (id decision not found and isNoWait is false)
     */
    private ArgContainer getTaskValue(UUID taskId, UUID processId, boolean isNoWait) throws IllegalStateException {

        DecisionContainer taskDecision = taskDao.getDecisionContainer(taskId, processId);

        if (taskDecision == null) {
            if (isNoWait) {
                // value may be null for NoWait promises
                // leave it in peace...
                return null;
            }
            throw new IllegalStateException("Decision not found for not @NoWait task [" + taskId + "] processId [" +
                    processId + "]");
        }

        logger.debug("taskDecision of taskId [{}] is [{}]", taskId, taskDecision);

        ArgContainer result;

        if (taskDecision.containsError()) {
            result = taskDecision.getValue();
            result.setErrorContainer(taskDecision.getErrorContainer());
        } else {
            result = taskDecision.getValue();
            if (result != null && result.isPromise() && !result.isReady()) {
                logger.debug("getTaskValue([{}]) argContainer.isPromise() && !argContainer.isReady(). arg[{}]", taskId, result);

                result = getTaskValue(result.getTaskId(), processId, isNoWait);
            }
        }

        logger.debug("getTaskValue({}) returns argContainer = [{}]", taskId, result);
        return result;
    }


    @Override
    public TaskContainer getTask(UUID taskId, UUID processId) {
        TaskContainer task = taskDao.getTask(taskId, processId);
        logger.debug("Task received by uuid[{}], is[{}]", taskId, task);
        return task;
    }

    @Override
    public List<TaskContainer> findTasks(TaskSearchCommand command) {
        return taskDao.findTasks(command);
    }

    @Override
    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize) {
        return taskDao.listTasks(pageNumber, pageSize);
    }

    @Override
    public Collection<TaskContainer> getRepeatedTasks(int iterationCount) {
        return taskDao.getRepeatedTasks(iterationCount);
    }

    @Override
    public Collection<TaskContainer> getProcessTasks(Collection<UUID> processTaskIds, UUID processId) {
        Collection<TaskContainer> tasks = new LinkedList<>();

        for (UUID taskId : processTaskIds) {
            tasks.add(taskDao.getTask(taskId, processId));
        }

        return tasks;
    }

    @Override
    public Decision finishTask(DecisionContainer taskDecision) {
        logger.debug("finishTask() taskDecision [{}]", taskDecision);

        return taskDao.finishTask(taskDecision);
    }

    @Override
    public void addNewTasks(DecisionContainer taskDecision) {
        logger.debug("addNewTasks() taskDecision [{}]", taskDecision);

        TaskContainer[] taskContainers = taskDecision.getTasks();
        if (taskContainers != null) {
            for (TaskContainer taskContainer : taskContainers) {
                taskDao.addTask(taskContainer);
            }
        }
    }

    @Override
    public boolean retryTask(UUID taskId, UUID processId) {
        return taskDao.retryTask(taskId, processId);
    }

    @Override
    public boolean restartTask(UUID taskId, UUID processId, boolean force, boolean ifFatalError) {
        return taskDao.restartTask(taskId, processId, force, ifFatalError);
    }

    @Override
    public Decision getDecision(UUID taskId, UUID processId) {
        return taskDao.getDecision(taskId, processId);
    }

    @Override
    public DecisionContainer getDecisionContainer(UUID taskId, UUID processId) {
        return taskDao.getDecisionContainer(taskId, processId);
    }

    @Override
    public List<TaskContainer> getAllRunProcesses() {
        return null;
    }

    @Override
    public List<DecisionContainer> getAllTaskDecisions(UUID processId) {
        return null;
    }

    @Override
    public void finishProcess(UUID processId, Collection<UUID> finishedTaskIds) {
        taskDao.archiveProcessData(processId, finishedTaskIds);
    }

    @Override
    public void updateTaskDecision(DecisionContainer taskDecision) {
        taskDao.updateTaskDecision(taskDecision);
    }


    public boolean isTaskReleased(UUID taskId, UUID processId) {
        return taskDao.isTaskReleased(taskId, processId);
    }
}
