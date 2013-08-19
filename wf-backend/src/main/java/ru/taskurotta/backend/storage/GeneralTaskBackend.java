package ru.taskurotta.backend.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.retriever.TaskInfoRetriever;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskType;

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
public class GeneralTaskBackend implements TaskBackend, TaskInfoRetriever {

    private final static Logger logger = LoggerFactory.getLogger(GeneralTaskBackend.class);

    private TaskDao taskDao;

    private CheckpointService checkpointService;

    public GeneralTaskBackend(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public GeneralTaskBackend(TaskDao taskDao, CheckpointService checkpointService) {
        this(taskDao);
        this.checkpointService = checkpointService;
    }

    @Override
    public void startProcess(TaskContainer taskContainer) {
        taskDao.addTask(taskContainer);
    }

    @Override
    public TaskContainer getTaskToExecute(UUID taskId, UUID processId) {
        logger.debug("getTaskToExecute(taskId[{}], processId[{}]) started", taskId, processId);

        TaskContainer task = getTask(taskId, processId);

        // WARNING: "task" object is the same instance as In memory data storage. So we should use  it deep clone
        // due guarantees for its immutability.

        if (task == null) {
            return null;
        }

        ArgContainer[] args = task.getArgs();

        if (args != null) {//updating ready Promises args into real values

            if (logger.isDebugEnabled()) {
                logger.debug("Task id[{}], type[{}], method[{}] args before swap processing [{}]", task.getTaskId(), task.getType(), task.getMethod(), Arrays.asList(args));
            }

            for (int i = 0; i < args.length; i++) {
                ArgContainer arg = args[i];

                if (args[i].isPromise()) {

                    args[i] = processPromiseArgValue(args[i], processId, task.getType());

                } else if (arg.isCollection()) {//can be collection of promises, case should be checked
                    ArgContainer[] compositeValue = arg.getCompositeValue();
                    for (int j = 0; j < compositeValue.length; j++) {
                        ArgContainer innerArg = compositeValue[j];
                        if (innerArg.isPromise()) {
                            compositeValue[j] = processPromiseArgValue(innerArg, processId, task.getType());
                        }
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Task id[{}], type[{}], method[{}] args after swap processing [{}]", task.getTaskId(), task.getType(), task.getMethod(), Arrays.asList(args));
            }
        }

        //Setting TASK_START checkpoint
        if (checkpointService != null) {
            Checkpoint startCheckpoint = new Checkpoint(TimeoutType.TASK_START_TO_CLOSE, taskId, task.getProcessId(), task.getActorId(), System.currentTimeMillis());
            checkpointService.addCheckpoint(startCheckpoint);
        }


        return task;
    }


    private ArgContainer processPromiseArgValue(ArgContainer pArg, UUID processId, TaskType taskType) {
        ArgContainer result = null;

        if (pArg.isReady() && !TaskType.DECIDER_ASYNCHRONOUS.equals(taskType)) {//Promise.asPromise() was used as an argument, so there is no taskValue, it is simply Promise wrapper for a worker
            logger.debug("Got initialized promise, switch it to value");
            return pArg.updateType(false);//simply strip of promise wrapper
        }

        ArgContainer taskValue = getTaskValue(pArg.getTaskId(), processId);//try to find promise value obtained by its task result

        if (taskValue == null) {
            // value may be null for NoWait promises
            // leave it in peace...
            return pArg;
        }

        ArgContainer newArg = new ArgContainer(taskValue);
        if (TaskType.DECIDER_ASYNCHRONOUS.equals(taskType)) {

            // set real value into promise for Decider tasks
            newArg.setPromise(true);
            newArg.setTaskId(pArg.getTaskId());
        } else {

            // swap promise with real value for Actor tasks
            newArg.setPromise(false);
        }

        return newArg;

    }

    private static boolean isDeciderTaskType(TaskType taskType) {
        return TaskType.DECIDER_ASYNCHRONOUS.equals(taskType) || TaskType.DECIDER_START.equals(taskType);
    }

    private ArgContainer getTaskValue(UUID taskId, UUID processId) {

        logger.debug("getTaskValue([{}])", taskId);
        if (taskId == null) {
            throw new IllegalStateException("Cannot find value for NULL taskId");
        }
        DecisionContainer taskDecision = taskDao.getDecision(taskId, processId);

        logger.debug("taskDecision getted for[{}] is [{}]", taskId, taskDecision);


        if (taskDecision == null) {
            logger.debug("getTaskValue() taskDecision == null");
            return null;

            //HACK FOR DEBUG
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            taskDecision = taskDao.getDecision(taskId, processId);
//            if(taskDecision == null) {
//                logger.debug("STILL NULL (bug in dependency?)!");
//                return null;
//            } else {
//                logger.debug("RERUN(bug in non-locking DAO operations?): taskDecision getted for[{}] is [{}]", taskId, taskDecision); //<-- this message in log! bug detected
//            }
        }

        ArgContainer result = taskDecision.getValue();
        if (result != null && result.isPromise() && !result.isReady()) {
            logger.debug("getTaskValue([{}]) argContainer.isPromise() && !argContainer.isReady(). arg[{}]", taskId, result);
            result = getTaskValue(result.getTaskId(), processId);
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
    public void addDecision(DecisionContainer taskDecision) {

        logger.debug("addDecision() taskDecision [{}]", taskDecision);

        TaskContainer task = null;

        if (checkpointService != null) {
            task = taskDao.getTask(taskDecision.getTaskId(), taskDecision.getProcessId());

            if (task != null) {
                checkpointService.addCheckpoint(new Checkpoint(TimeoutType.TASK_RELEASE_TO_COMMIT, task.getTaskId(), task.getProcessId(), task.getActorId(), System.currentTimeMillis()));
            } else {
                logger.debug("Task with null value getted for decision[{}]", taskDecision);
            }

        }

        taskDao.addDecision(taskDecision);

        // increment number of attempts for error tasks with retry policy
        if (taskDecision.containsError() && taskDecision.getRestartTime() != -1) {

            if (task == null) {
                task = taskDao.getTask(taskDecision.getTaskId(), taskDecision.getProcessId());
            }

            // TODO: should be optimized
            task.incrementNumberOfAttempts();
            taskDao.updateTask(task);
        }

        TaskContainer[] taskContainers = taskDecision.getTasks();
        if (taskContainers != null) {
            for (TaskContainer taskContainer : taskContainers) {
                taskDao.addTask(taskContainer);
            }
        }
    }

    @Override
    public DecisionContainer getDecision(UUID taskId, UUID processId) {
        return taskDao.getDecision(taskId, processId);
    }

    @Override
    public void addDecisionCommit(DecisionContainer taskDecision) {
        //Removing checkpoints

        if (checkpointService == null) {
            return;
        }

        checkpointService.removeTaskCheckpoints(taskDecision.getTaskId(), taskDecision.getProcessId(), TimeoutType.TASK_START_TO_CLOSE);
        checkpointService.removeTaskCheckpoints(taskDecision.getTaskId(), taskDecision.getProcessId(), TimeoutType.TASK_SCHEDULE_TO_CLOSE);
        checkpointService.removeTaskCheckpoints(taskDecision.getTaskId(), taskDecision.getProcessId(), TimeoutType.TASK_RELEASE_TO_COMMIT);
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


    public boolean isTaskReleased(UUID taskId, UUID processId) {
        return taskDao.isTaskReleased(taskId, processId);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }


}
