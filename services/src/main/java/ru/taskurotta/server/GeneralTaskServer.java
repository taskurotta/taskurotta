package ru.taskurotta.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.model.DependencyDecision;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.console.model.BrokenProcess;
import ru.taskurotta.service.storage.BrokenProcessService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.queue.TaskQueueItem;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:04 PM
 */
public class GeneralTaskServer implements TaskServer {

    private static final Logger logger = LoggerFactory.getLogger(GeneralTaskServer.class);

    protected ProcessService processService;
    protected TaskService taskService;
    protected QueueService queueService;
    protected DependencyService dependencyService;
    protected ConfigService configService;
    protected BrokenProcessService brokenProcessService;
    protected GarbageCollectorService garbageCollectorService;

    /*
     *  For tests ONLY
     */
    public GeneralTaskServer() {}

    public GeneralTaskServer(ServiceBundle serviceBundle) {
        this.processService = serviceBundle.getProcessService();
        this.taskService = serviceBundle.getTaskService();
        this.queueService = serviceBundle.getQueueService();
        this.dependencyService = serviceBundle.getDependencyService();
        this.configService = serviceBundle.getConfigService();
        this.brokenProcessService = serviceBundle.getBrokenProcessService();
        this.garbageCollectorService = serviceBundle.getGarbageCollectorService();
    }

    public GeneralTaskServer(ProcessService processService, TaskService taskService, QueueService queueService,
                             DependencyService dependencyService, ConfigService configService, BrokenProcessService brokenProcessService,
                             GarbageCollectorService garbageCollectorService) {
        this.processService = processService;
        this.taskService = taskService;
        this.queueService = queueService;
        this.dependencyService = dependencyService;
        this.configService = configService;
        this.brokenProcessService = brokenProcessService;
        this.garbageCollectorService = garbageCollectorService;
    }

    @Override
    public void startProcess(TaskContainer task) {

        // some consistence check
        if (!(task.getType().equals(TaskType.DECIDER_START) || task.getType().equals(TaskType.WORKER_SCHEDULED))) {
            // TODO: send error to client
            throw new IllegalStateException("Can not start process with task type["+task.getType()+"]. Should be one of [" + TaskType.DECIDER_START + ", " + TaskType.WORKER_SCHEDULED + "]");
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
    }


    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        String actorId = ActorUtils.getActorId(actorDefinition);

        if (configService.isActorBlocked(actorId)) {
            logger.warn("Rejected poll request from blocked actor {}", actorDefinition);
            return null;
        }

        // atomic statement
        TaskQueueItem item = queueService.poll(ActorUtils.getActorId(actorDefinition), actorDefinition.getTaskList());
        if (item == null) {
            return null;
        }

        // idempotent statement
        TaskContainer result = taskService.getTaskToExecute(item.getTaskId(), item.getProcessId());
        if (result == null) {
            logger.error("Failed to get task for queue item ["+item+"] from store. Inconsistent state: possible data loss?");
        }
        return result;
    }

    @Override
    public void release(DecisionContainer taskDecision) {

        if (configService.isActorBlocked(taskDecision.getActorId())) {
            logger.warn("Rejected  blocked actor [{}] release request", taskDecision.getActorId());
            return;
        }

        // save it firstly
        taskService.addDecision(taskDecision);
        processDecision(taskDecision);
    }

    /**
     *
     */
    public void processDecision(DecisionContainer taskDecision) {

        UUID taskId = taskDecision.getTaskId();
        UUID processId = taskDecision.getProcessId();

        logger.trace("#[{}]/[{}]: start processing taskDecision = [{}]", processId, taskId, taskDecision);

        if (taskDecision.containsError()) {

            TaskContainer task = taskService.getTask(taskId, processId);
            logger.trace("#[{}]/[{}]: after get taskDecision with error again get task = [{}]", processId, taskId, task);

            long restartTime = taskDecision.getRestartTime();
            if (restartTime != TaskDecision.NO_RESTART) {

                // enqueue task immediately if needed
                logger.debug("#[{}]/[{}]: again enqueued error task = [{}]", processId, taskId, task);
                enqueueTask(taskId, processId, task.getActorId(), restartTime, getTaskList(task));
                return;

            } else {

                if (!task.isUnsafe() || !isErrorMatch(task, taskDecision.getErrorContainer())) {
                    saveBrokenProcess(taskDecision);
                    processService.markProcessAsBroken(processId);
                    logger.debug("Process [{}] marked as broken: taskDecision = [{}], task = [{}]", processId, taskDecision, task);
                    return;
                }

            }
        }

        if (unsafePromiseSentToWorker(taskDecision.getTasks())) {
            saveBrokenProcess(taskDecision);
            processService.markProcessAsBroken(processId);
            return;
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
                enqueueTask(readyTaskId, task.getProcessId(), task.getActorId(), task.getStartTime(), getTaskList(task));
            }
        }

        if (dependencyDecision.isProcessFinished()) {
            processService.finishProcess(processId, dependencyDecision.getFinishedProcessValue());
            taskService.finishProcess(processId, dependencyService.getGraph(processId).getProcessTasks());
            garbageCollectorService.delete(processId);
        }

        logger.debug("#[{}]/[{}]: finish processing taskDecision = [{}]", processId, taskId, taskDecision);
    }

    private static boolean isErrorMatch(TaskContainer task, ErrorContainer error) {
        if (!task.isUnsafe()) {
            return false;   // no one error match for safe tasks
        }

        String[] taskFailTypes = task.getFailTypes();
        if (null == taskFailTypes || taskFailTypes.length == 0) {
            return true;    // no restrictions defined. all errors matches
        }

        Set<String> failTypes = new HashSet<>(Arrays.asList(taskFailTypes));

        for (String errorName : error.getClassNames()) {
            if (failTypes.contains(errorName)){
                return true;
            }
        }
        return false;
    }

    private boolean unsafePromiseSentToWorker(TaskContainer[] tasks) {
        if (null == tasks) {
            return false;
        }

        HashMap<UUID, TaskContainer> tasksMap = new HashMap<>(tasks.length);
        for (TaskContainer newTask : tasks) {
            tasksMap.put(newTask.getTaskId(), newTask);
        }

        for (TaskContainer newTask : tasks) {
            if (!TaskType.WORKER.equals(newTask.getType())) {
                continue;
            }

            ArgContainer[] args = newTask.getArgs();
            if (null != args) {
                for (ArgContainer arg : args) {
                    if (arg.isPromise()) {
                        TaskContainer argTask = tasksMap.get(arg.getTaskId());
                        if (argTask == null) {
                            argTask = taskService.getTask(arg.getTaskId(), newTask.getProcessId());
                        }
                        if (null != argTask && argTask.isUnsafe()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    private void saveBrokenProcess(DecisionContainer taskDecision) {
        UUID processId = taskDecision.getProcessId();
        BrokenProcess brokenProcess = new BrokenProcess();
        brokenProcess.setTime(System.currentTimeMillis());
        brokenProcess.setProcessId(processId);
        brokenProcess.setBrokenActorId(taskDecision.getActorId());

        TaskContainer startTask = processService.getStartTask(processId);
        if (null != startTask) {
            brokenProcess.setStartActorId(startTask.getActorId());
        }

        ErrorContainer errorContainer = taskDecision.getErrorContainer();
        if (null != errorContainer) {
            brokenProcess.setErrorClassName(errorContainer.getClassName());
            brokenProcess.setErrorMessage(errorContainer.getMessage());
            brokenProcess.setStackTrace(errorContainer.getStackTrace());
        }
        brokenProcessService.save(brokenProcess);
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
            if (taskOptionsContainer.getActorSchedulingOptions() != null) {
                taskList = taskOptionsContainer.getActorSchedulingOptions().getTaskList();
            }
        }

        return taskList;
    }
}
