package ru.taskurotta.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.policy.PolicyConstants;
import ru.taskurotta.policy.retry.TimeRetryPolicyBase;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.model.DependencyDecision;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.queue.TaskQueueItem;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.*;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;
import ru.taskurotta.util.RetryPolicyConfigUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:04 PM
 */
public class GeneralTaskServer implements TaskServer {

    private static final Logger logger = LoggerFactory.getLogger(GeneralTaskServer.class);

    public static final AtomicInteger startedProcessesCounter = new AtomicInteger();
    public static final AtomicInteger finishedProcessesCounter = new AtomicInteger();
    public static final AtomicInteger brokenProcessesCounter = new AtomicInteger();

    public static final AtomicInteger startedDistributedTasks = new AtomicInteger();
    public static final AtomicInteger finishedDistributedTasks = new AtomicInteger();

    protected ProcessService processService;
    protected TaskService taskService;
    protected QueueService queueService;
    protected DependencyService dependencyService;
    protected ConfigService configService;
    protected InterruptedTasksService interruptedTasksService;
    protected GarbageCollectorService garbageCollectorService;

    /*
     *  For tests ONLY
     */
    public GeneralTaskServer() {
    }

    public GeneralTaskServer(ServiceBundle serviceBundle) {
        this.processService = serviceBundle.getProcessService();
        this.taskService = serviceBundle.getTaskService();
        this.queueService = serviceBundle.getQueueService();
        this.dependencyService = serviceBundle.getDependencyService();
        this.configService = serviceBundle.getConfigService();
        this.interruptedTasksService = serviceBundle.getInterruptedTasksService();
        this.garbageCollectorService = serviceBundle.getGarbageCollectorService();
    }

    public GeneralTaskServer(ProcessService processService, TaskService taskService, QueueService queueService,
                             DependencyService dependencyService, ConfigService configService, InterruptedTasksService interruptedTasksService,
                             GarbageCollectorService garbageCollectorService) {
        this.processService = processService;
        this.taskService = taskService;
        this.queueService = queueService;
        this.dependencyService = dependencyService;
        this.configService = configService;
        this.interruptedTasksService = interruptedTasksService;
        this.garbageCollectorService = garbageCollectorService;
    }

    @Override
    public void startProcess(TaskContainer task) {

        // some consistence check
        if (!(task.getType().equals(TaskType.DECIDER_START) || task.getType().equals(TaskType.WORKER_SCHEDULED))) {
            // TODO: send error to client
            throw new IllegalStateException("Can not start process with task type[" + task.getType() + "]. Should be one of [" + TaskType.DECIDER_START + ", " + TaskType.WORKER_SCHEDULED + "]");
        }

        // registration of new process
        // atomic statement
        processService.startProcess(task);

        // inform taskService about new process
        // idempotent statement
        taskService.startProcess(task);

        // inform dependencyService about new process
        // idempotent statement
        dependencyService.startProcess(task);

        // we assume that new process task has no dependencies and it is ready to enqueue.
        // idempotent statement
        enqueueTask(task.getTaskId(), task.getProcessId(), task.getActorId(), task.getStartTime(), getTaskList(task));

        startedProcessesCounter.incrementAndGet();
    }


    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        String actorId = ActorUtils.getActorId(actorDefinition);

        if (configService.isActorBlocked(actorId)) {
            logger.debug("Rejected poll request from blocked actor {}", actorDefinition);
            // todo: this is a hack to avoid overflow of server logs
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException ignore) {
            }
            return null;
        }

        while (true) {

            TaskQueueItem item = queueService.poll(ActorUtils.getActorId(actorDefinition), actorDefinition.getTaskList());
            if (item == null) {
                return null;
            }

            TaskContainer result = taskService.getTaskToExecute(item.getTaskId(), item.getProcessId(), false);
            if (result == null) {
                logger.warn("Failed to get task for queue item [" + item + "] from store");
                // todo: server response can be very slowly in this case with huge amount of broken tasks
                continue;
            }

            return result;
        }
    }

    @Override
    public void release(DecisionContainer taskDecision) {

        // todo: should we block release of task? Or it should be another kind of block?
        // todo: in general situation it is not good idea to drop release of task
//        if (configService.isActorBlocked(taskDecision.getActorId())) {
//            logger.warn("Rejected  blocked actor [{}] release request", taskDecision.getActorId());
//            return;
//        }

        // save it firstly
        if (!taskService.finishTask(taskDecision)) {
            logger.warn("{}/{} Task decision can not be saved", taskDecision.getTaskId(), taskDecision.getProcessId());
            return;
        }

        processDecision(taskDecision);
    }


    public void processDecision(UUID taskId, UUID processId) {

        DecisionContainer taskDecision = taskService.getDecision(taskId, processId);

        if (taskDecision == null) {
            throw new IllegalStateException("Task decision not found. taskId = " + taskId + " processId = " +
                    processId);
        }


        processDecision(taskDecision);
    }

    public void processDecision(DecisionContainer taskDecision) {

        UUID taskId = taskDecision.getTaskId();
        UUID processId = taskDecision.getProcessId();

        logger.trace("#[{}]/[{}]: start processing taskDecision = [{}]", processId, taskId, taskDecision);

        if (taskDecision.containsError()) {

            long restartTime = TaskDecision.NO_RESTART;

            TaskContainer task = taskService.getTask(taskId, processId);
            logger.trace("#[{}]/[{}]: after get taskDecision with error again get task = [{}]", processId, taskId, task);

            RetryPolicyConfigContainer deciderRetryPolicyConfig = null;

            TaskOptionsContainer taskOptionsContainer = task.getOptions();
            if (taskOptionsContainer != null) {
                TaskConfigContainer taskConfigContainer = taskOptionsContainer.getTaskConfigContainer();
                if (taskConfigContainer != null) {
                    deciderRetryPolicyConfig = taskConfigContainer.getRetryPolicyConfigContainer();
                }
            }

            if (deciderRetryPolicyConfig != null) {
                if (isErrorMatch(deciderRetryPolicyConfig, taskDecision.getErrorContainer())) {

                    restartTime = getRestartTime(task, deciderRetryPolicyConfig);
                }
            } else {
                restartTime = taskDecision.getRestartTime();
            }

            if (restartTime != TaskDecision.NO_RESTART) {

                // enqueue task immediately if needed
                logger.debug("#[{}]/[{}]: enqueue error task = [{}]", processId, taskId, task);

                if (taskService.retryTask(taskId, processId, restartTime)) {
                    enqueueTask(taskId, processId, task.getActorId(), restartTime, getTaskList(task));
                } else {
                    logger.warn("{}/{} Can not prepare task to retry. Operation taskService.retryTask() return is " +
                            "false.", processId, taskId);
                }

                return;
            }

            if (!(task.isUnsafe() && isErrorMatch(task, taskDecision.getErrorContainer()))) {
                markProcessAsBroken(taskDecision);

                logger.debug("Process [{}] marked as broken: taskDecision = [{}], task = [{}]", processId, taskDecision, task);
                return;
            }


        } else {

            if (unsafePromiseSentToWorker(taskDecision.getTasks())) {
                ErrorContainer errorContainer = new ErrorContainer(new String[]{"java.lang" +
                        ".IllegalArgumentException"}, "Unsafe promise sent to worker. Actor decision: " + taskDecision
                        .getActorId(), "", true);
                taskDecision.setErrorContainer(errorContainer);

                markProcessAsBroken(taskDecision);
                return;
            }

        }

        // idempotent statement
        DependencyDecision dependencyDecision = dependencyService.applyDecision(taskDecision);
        logger.trace("#[{}]/[{}]: after apply taskDecision, get dependencyDecision = [{}]", processId, taskId, dependencyDecision);

        if (dependencyDecision.isFail()) {

            logger.debug("#[{}]/[{}]: failed dependencyDecision = [{}]", processId, taskId, dependencyDecision);
            return;
        }

        Set<UUID> readyTasks = dependencyDecision.getReadyTasks();
        if (readyTasks != null) {
            for (UUID readyTaskId : readyTasks) {
                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer task = taskService.getTask(readyTaskId, processId);
                if (task == null) {

                    if (task == null) {
                        logger.error("#[{}]/[{}]: failed to enqueue task. ready task not found in the taskService" +
                                        ". dependencyDecision = [{}]",
                                processId, taskId, dependencyDecision);

                        // wait recovery for this process
                        // todo: throw exception?
                        continue;
                    }
                }

                enqueueTask(readyTaskId, task.getProcessId(), task.getActorId(), task.getStartTime(), getTaskList(task));
            }
        }

        if (dependencyDecision.isProcessFinished()) {
            finishedProcessesCounter.incrementAndGet();

            processService.finishProcess(processId, dependencyDecision.getFinishedProcessValue());
            taskService.finishProcess(processId, dependencyService.getGraph(processId).getProcessTasks());
            garbageCollectorService.collect(processId);
        }

        logger.debug("#[{}]/[{}]: finish processing taskDecision = [{}]", processId, taskId, taskDecision);

    }

    private void markProcessAsBroken(DecisionContainer taskDecision) {
        UUID processId = taskDecision.getProcessId();

        // save decision with fatal flag
        taskDecision.getErrorContainer().setFatalError(true);
        taskService.updateTaskDecision(taskDecision);

        // increments stat counter
        brokenProcessesCounter.incrementAndGet();

        // save interrupted task information
        InterruptedTask itdTask = new InterruptedTask();
        itdTask.setTime(System.currentTimeMillis());
        itdTask.setProcessId(processId);
        itdTask.setTaskId(taskDecision.getTaskId());
        itdTask.setActorId(taskDecision.getActorId());

        TaskContainer startTask = processService.getStartTask(processId);
        if (startTask != null) {
            itdTask.setStarterId(startTask.getActorId());
        }

        ErrorContainer errorContainer = taskDecision.getErrorContainer();
        if (errorContainer != null) {
            itdTask.setErrorClassName(errorContainer.getClassName());
            itdTask.setErrorMessage(errorContainer.getMessage());
            itdTask.setStackTrace(errorContainer.getStackTrace());
        }
        interruptedTasksService.save(itdTask);

        // mark process as broken
        processService.markProcessAsBroken(processId);
    }

    private long getRestartTime(TaskContainer task, RetryPolicyConfigContainer retryPolicyConfig) {
        if (retryPolicyConfig == null) {
            return PolicyConstants.NONE;
        }

        long recordedFailure = System.currentTimeMillis();
        TimeRetryPolicyBase timeRetryPolicyBase = RetryPolicyConfigUtil.buildTimeRetryPolicy(retryPolicyConfig);
        long customRestartTime = timeRetryPolicyBase.nextRetryDelaySeconds(task.getStartTime(), recordedFailure, task.getErrorAttempts());
        if (customRestartTime == PolicyConstants.NONE) {
            return PolicyConstants.NONE;
        }

        return recordedFailure + customRestartTime * 1000;
    }

    private static boolean isErrorMatch(RetryPolicyConfigContainer retryPolicyConfig, ErrorContainer error) {
        for (String errorName : error.getClassNames()) {
            if (RetryPolicyConfigUtil.isRetryable(errorName, retryPolicyConfig)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isErrorMatch(TaskContainer task, ErrorContainer error) {
        if (!task.isUnsafe()) {
            return false;   // no one error match for safe tasks
        }

        String[] taskFailTypes = task.getFailTypes();
        if (taskFailTypes == null || taskFailTypes.length == 0) {
            return true;    // no restrictions defined. all errors matches
        }

        Set<String> failTypes = new HashSet<>(Arrays.asList(taskFailTypes));

        for (String errorName : error.getClassNames()) {
            if (failTypes.contains(errorName)) {
                return true;
            }
        }
        return false;
    }

    private boolean unsafePromiseSentToWorker(TaskContainer[] tasks) {
        if (tasks == null) {
            return false;
        }

        HashMap<UUID, TaskContainer> id2taskMap = new HashMap<>(tasks.length);
        for (TaskContainer newTask : tasks) {
            id2taskMap.put(newTask.getTaskId(), newTask);
        }

        for (TaskContainer newTask : tasks) {
            if (!TaskType.WORKER.equals(newTask.getType())) {
                continue;
            }

            ArgContainer[] args = newTask.getArgs();
            if (args == null) {
                continue;
            }

            for (ArgContainer arg : args) {
                if (arg.isPromise() && !arg.isReady()) {
                    TaskContainer argTask = id2taskMap.get(arg.getTaskId());
                    if (argTask == null) {
                        argTask = taskService.getTask(arg.getTaskId(), newTask.getProcessId());
//                        if (argTask == null) {
//                            System.err.println("Task with null argTask : " + newTask);
//                        }
                    }
                    if (argTask != null && argTask.isUnsafe()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Send task to the queue for processing
     *
     * @param startTime time to start delayed task. set to 0 to start it immediately
     * @param taskList  -
     */
    protected void enqueueTask(UUID taskId, UUID processId, String actorId, long startTime, String taskList) {

        queueService.enqueueItem(actorId, taskId, processId, startTime, taskList);

    }


    protected String getTaskList(TaskContainer taskContainer) {
        String taskList = null;
        if (taskContainer.getOptions() != null) {
            TaskOptionsContainer taskOptionsContainer = taskContainer.getOptions();
            if (taskOptionsContainer.getTaskConfigContainer() != null) {
                taskList = taskOptionsContainer.getTaskConfigContainer().getTaskList();
            }
        }

        return taskList;
    }
}
