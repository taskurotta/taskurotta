package ru.taskurotta.service.recovery.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.recovery.RecoveryService;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskDao;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: stukushin
 * Date: 21.10.13
 * Time: 18:24
 */
public class RecoveryServiceImpl implements RecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryServiceImpl.class);

    public static AtomicInteger recoveredProcessesCounter = new AtomicInteger();
    public static AtomicInteger recoveredTasksCounter = new AtomicInteger();
    public static AtomicInteger restartedBrokenTasks = new AtomicInteger();
    public static AtomicInteger recoveredInterruptedTasksCounter = new AtomicInteger();
    public static AtomicInteger recoveredProcessDecisionCounter = new AtomicInteger();
    public static AtomicInteger restartedIncompleteTasksCounter = new AtomicInteger();

    private GeneralTaskServer generalTaskServer;
    private QueueService queueService;
    private DependencyService dependencyService;
    private ProcessService processService;
    private TaskService taskService;
    private TaskDao taskDao;
    private GraphDao graphDao;
    private InterruptedTasksService interruptedTasksService;
    private GarbageCollectorService garbageCollectorService;
    // time out between recovery process in milliseconds
    private long recoveryProcessChangeTimeout;
    private long findIncompleteProcessPeriod;
    private long timeBeforeDeleteFinishedProcess;
    private long timeBeforeManualDeleteProcess;

    public RecoveryServiceImpl() {
    }

    public RecoveryServiceImpl(GeneralTaskServer generalTaskServer, QueueService queueService,
                               DependencyService dependencyService, ProcessService processService,
                               TaskService taskService, TaskDao taskDao,
                               GraphDao graphDao, InterruptedTasksService interruptedTasksService,
                               GarbageCollectorService garbageCollectorService, long recoveryProcessChangeTimeout,
                               long findIncompleteProcessPeriod, long timeBeforeDeleteFinishedProcess,
                               long timeBeforeManualDeleteProcess) {

        // todo: THIS IS A HUCK. SERVICES STRUCTURE SHOULD BE OPTIMIZED!!!
        this.generalTaskServer = generalTaskServer;

        this.queueService = queueService;
        this.dependencyService = dependencyService;
        this.processService = processService;
        this.taskService = taskService;
        this.taskDao = taskDao;
        this.graphDao = graphDao;
        this.interruptedTasksService = interruptedTasksService;
        this.garbageCollectorService = garbageCollectorService;
        this.recoveryProcessChangeTimeout = recoveryProcessChangeTimeout;
        this.findIncompleteProcessPeriod = findIncompleteProcessPeriod;
        this.timeBeforeDeleteFinishedProcess = timeBeforeDeleteFinishedProcess;
        this.timeBeforeManualDeleteProcess = timeBeforeManualDeleteProcess;
    }

    @Override
    public boolean resurrectProcess(final UUID processId) {

        processService.lock(processId);

        try {
            return resurrectProcessInternal(processId);
        } finally {
            processService.unlock(processId);
        }

    }

    private boolean resurrectProcessInternal(final UUID processId) {
        logger.trace("#[{}]: try to resurrect process", processId);


        // skip broken and already finished process
        Process process = processService.getProcess(processId);
        if (process == null || process.getState() == Process.FINISH || process.getState() == Process.BROKEN) {
            return false;
        }


        // val=true if some tasks have been placed to queue
        boolean result = false;

        Graph graph = dependencyService.getGraph(processId);


        if (graph == null) {
            // have only process service info => restart whole process

            logger.warn("#[{}]: graph was not found (possible data loss?), try to restart process from start task", processId);
            result = restartProcessFromBeginning(processId);

        } else if (graph.isFinished()) {
            // process is already finished => just mark process as finished

            // check if Process is finished because Graph are marked as finished before Process

            if (process.getState() == Process.FINISH) {
                logger.debug("#[{}]: is finished, just skip it", processId);
                return false;
            }

            logger.debug("#[{}]: isn't finished, but graph is finished, force finish process", processId);
            TaskContainer startTaskContainer = processService.getStartTask(processId);
            finishProcess(processId, startTaskContainer.getTaskId(), graph.getProcessTasks());

        } else if (hasRecentActivity(graph)) {
            // was restarted or updated recently  => leave it alone for now

            logger.debug("#[{}]: graph was recently applied or recovered, skip it", processId);

        } else {

            final Map<UUID, Long> allReadyTaskIds = graph.getAllReadyItems();

            if (logger.isDebugEnabled()) {
                logger.debug("#[{}]: found [{}] not finished taskIds", processId, allReadyTaskIds.size());
            }

            // apply decisions for not finished tasks

            for (Iterator<Map.Entry<UUID, Long>> iterator = allReadyTaskIds.entrySet().iterator(); iterator.hasNext(); ) {
                UUID taskId = iterator.next().getKey();

                DecisionContainer decisionContainer = taskService.getDecisionContainer(taskId, processId);
                if (decisionContainer != null) {

                    logger.debug("#[{}]: task is finished but still ready in Graph. Apply task decision {}",
                            processId, taskId);

                    // task is finished but still ready in Graph.
                    try {
                        if (generalTaskServer.processDecision(taskId, processId)) {
                            iterator.remove();
                            recoveredProcessDecisionCounter.incrementAndGet();
                            result = true;
                        }
                    } catch (RuntimeException ex) {
                        logger.error("Can not process decision", ex);
                    }
                }
            }

            if (allReadyTaskIds.isEmpty()) {
                return result;
            }

            // require restart => try to find process's tasks for restart

            Collection<TaskContainer> taskContainers = getIncompleteTaskContainers(allReadyTaskIds, processId);
            // taskContainers == null if TaskContainer not found in store for least one taskId
            if (taskContainers == null) {
                // there is a problem in task store => restart process

                logger.warn("#[{}]: task containers were not found (possible data loss?), try to restart process from start task", processId);
                result = result || restartProcessFromBeginning(processId);

            } else {
                // restart unfinished tasks

                int recoveredTasks = restartProcessTasks(taskContainers, processId);
                recoveredTasksCounter.addAndGet(recoveredTasks);

                result = result || recoveredTasks > 0;

                boolean graphUpdated = false;

                logger.debug("#[{}]: try to update graph", processId);

                if (result) {
                    graphUpdated = dependencyService.changeGraph(new GraphDao.Updater() {
                        @Override
                        public UUID getProcessId() {
                            return processId;
                        }

                        @Override
                        public boolean apply(Graph graph) {
                            graph.setTouchTimeMillis(System.currentTimeMillis());

                            logger.debug("#[{}]: update touch time to [{} ({})]", processId, graph.getTouchTimeMillis());
                            return true;
                        }
                    });
                }
                logger.debug("#[{}]: has been recovered, graph update result [{}]", processId, graphUpdated);

            }

        }

        return result;
    }

    private void markProcessAsBroken(DecisionContainer taskDecision) {
        UUID processId = taskDecision.getProcessId();

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
        String message = null;
        String stacktrace = null;
        if (errorContainer != null) {
            itdTask.setErrorClassName(errorContainer.getClassName());
            message = errorContainer.getMessage();
            itdTask.setErrorMessage(TransportUtils.trimToLength(message, InterruptedTasksService.MESSAGE_MAX_LENGTH));
            stacktrace = errorContainer.getStackTrace();
        }
        interruptedTasksService.save(itdTask, message, stacktrace);

        // mark process as broken
        processService.markProcessAsBroken(processId);
    }


    private boolean hasRecentActivity(Graph graph) {

        if (graph == null) {
            return false;
        }

        boolean result = false;

        long lastChange = Math.max(graph.getLastApplyTimeMillis(), graph.getTouchTimeMillis());
        if (lastChange > 0) {
            //has some modifications, check if they expired

            long changeTimeout = System.currentTimeMillis() - lastChange;
            logger.debug("#[{}]: activity check for graph: last change[{}], elapsed time[{}]", graph.getGraphId(),
                    lastChange, changeTimeout);

            // todo: may be we not need "recoveryProcessChangeTimeout" property?
            // we can have two properties: process-finish-timeout and process-idle-timeout.
            // And have different recovery strategies for each other.
            // to find processes of process-idle-timeout we can use mongodb Graph collection
            result = changeTimeout < recoveryProcessChangeTimeout;
        }

        return result;
    }

    @Override
    public Collection<UUID> resurrectProcesses(Collection<UUID> processIds) {
        Collection<UUID> successfullyRestartedProcesses = new ArrayList<>();

        for (UUID processId : processIds) {
            if (resurrectProcess(processId)) {
                successfullyRestartedProcesses.add(processId);
            }
        }

        return successfullyRestartedProcesses;
    }

    private int restartProcessTasks(Collection<TaskContainer> taskContainers, UUID processId) {
        logger.trace("#[{}]: try to restart task containers: [{}]", processId, taskContainers);

        int restartedTasks = 0;

        if (taskContainers != null && !taskContainers.isEmpty()) {

            // check tasks
            for (Iterator<TaskContainer> it = taskContainers.iterator(); it.hasNext(); ) {
                TaskContainer taskContainer = it.next();
                UUID taskId = taskContainer.getTaskId();
                long startTime = taskContainer.getStartTime();
                String taskList = TransportUtils.getTaskList(taskContainer);
                String actorId = taskContainer.getActorId();

                DecisionContainer decisionContainer = taskService.getDecisionContainer(taskId, processId);
                if (decisionContainer != null) {
                    ErrorContainer errorContainer = decisionContainer.getErrorContainer();
                    if (errorContainer != null && errorContainer.isFatalError()) {

                        // process is broken now. Skip it.
                        taskContainers = null;
                        continue;
                    }
                }

                long lastRecoveryStartTime = System.currentTimeMillis() - findIncompleteProcessPeriod;
                if (!isReadyToRecover(processId, taskId, startTime, actorId, taskList, lastRecoveryStartTime)) {
                    // remove not ready task from collection
                    it.remove();
                    continue;
                }

                logger.trace("#[{}]/[{}]: task is ready and can be restarted", processId, taskId);

                // try to prepare task
                boolean result = true;
                try {
                    if (taskService.getTaskToExecute(taskId, processId, true) == null) {
                        result = false;
                    }
                } catch (IllegalStateException ex) {
                    result = false;
                }

                // Need we restart process?
                if (!result) {
                    if (!restartProcessFromBeginning(processId)) {
                        logger.error("Can not restart process from beginning. Process has ready task without " +
                                "consistent arguments. Process id = {} task id = {}", processId, taskId);
                    }

                    // drop collection of tasks. We not need them in cause its process has been restarted.
                    taskContainers = null;
                    break;
                }

            }

            // Should we restart task?
            if (taskContainers != null) {

                // restart tasks
                for (TaskContainer taskContainer : taskContainers) {
                    UUID taskId = taskContainer.getTaskId();
                    long startTime = taskContainer.getStartTime();
                    String taskList = TransportUtils.getTaskList(taskContainer);
                    String actorId = taskContainer.getActorId();

                    if (taskService.restartTask(taskId, processId, false, false)) {

                        // task ready to enqueue
                        if (queueService.enqueueItem(actorId, taskId, processId, startTime, taskList)) {

                            logger.debug("#[{}]/[{}]: task container [{}] have been restarted", processId, taskId,
                                    taskContainer);

                            restartedTasks++;
                        } else {
                            logger.debug("#[{}]/[{}]: can not restart task. enqueue operation is false", processId,
                                    taskId, taskContainer);
                        }
                    } else {

                        // task is finished but still ready in Graph.
                        try {
                            generalTaskServer.processDecision(taskId, processId);
                        } catch (RuntimeException ex) {
                            logger.error("Can not process decision", ex);
                        }

                        recoveredProcessDecisionCounter.incrementAndGet();

                        logger.debug("#[{}]/[{}]: can not restart task. taskService.restartTask() operation is false",
                                processId, taskId, taskContainer);
                    }
                }
            }

        }

        logger.debug("#[{}]: complete restart of [{}] tasks", processId, restartedTasks);

        return restartedTasks;
    }

    @Override
    public boolean abortProcess(final UUID processId) {

        processService.lock(processId);

        try {
            boolean result = dependencyService.changeGraph(new GraphDao.Updater() {
                @Override
                public UUID getProcessId() {
                    return processId;
                }

                @Override
                public boolean apply(Graph graph) {
                    Set<UUID> finishedItems = graph.getFinishedItems();
                    deleteTasksAndDecisions(finishedItems, processId);

                    Set<UUID> notFinishedItems = graph.getNotFinishedItems().keySet();
                    deleteTasksAndDecisions(notFinishedItems, processId);

                    graphDao.deleteGraph(processId);

                    return false;
                }
            });

            garbageCollectorService.collect(processId, timeBeforeManualDeleteProcess);

            processService.markProcessAsAborted(processId);

            logger.info("Abort process [{}]", processId);

            return result;

        } finally {
            processService.unlock(processId);
        }
    }

    @Override
    public boolean restartInterruptedTask(final UUID processId, final UUID taskId) {
        processService.lock(processId);

        try {

            Graph graph = dependencyService.getGraph(processId);
            if (graph == null) {

                // recovery service should restart this process from beginning
                processService.markProcessAsStarted(processId);

                // remove garbage
                interruptedTasksService.delete(processId, taskId);
                return false;
            }

            long touchTime = System.currentTimeMillis();
            graph.setTouchTimeMillis(touchTime);

            if (taskService.restartTask(taskId, processId, false, true)) {
                TaskContainer tc = taskService.getTask(taskId, processId);
                if (tc != null && queueService.enqueueItem(tc.getActorId(), tc.getTaskId(), tc.getProcessId(), System.currentTimeMillis(), TransportUtils.getTaskList(tc))) {
                    interruptedTasksService.delete(processId, taskId);
                    restartedBrokenTasks.incrementAndGet();
                }

                // --process state change part--
                if (!hasOtherNotReadyFatalTasks(processId, taskId, graph.getAllReadyItems())) {
                    processService.markProcessAsStarted(processId);
                }
                // --/process state change part--

            } else {
                logger.debug("restartInterruptedTask(): Can not restart interrupted task. taskId = {} processId= {}",
                        taskId, processId);
            }



            return true;
        } finally {
            processService.unlock(processId);
        }

    }

    @Override
    public boolean reenqueueTask(UUID taskId, UUID processId) {
        TaskContainer taskContainer = taskService.getTask(taskId, processId);
        if (taskContainer == null) {
            logger.warn("{}/{}: not found task container", processId, taskId);
            return false;
        }

        if (taskService.restartTask(taskId, processId, false, false)) {
            String actorId = taskContainer.getActorId();
            long startTime = taskContainer.getStartTime();
            String taskList = TransportUtils.getTaskList(taskContainer);
            if (queueService.enqueueItem(actorId, taskId, processId, startTime, taskList)) {
                logger.debug("{}/{}: task container has been enqueued", processId, taskId);
            } else {
                logger.warn("{}/{}: can not restart task. Enqueue operation is false", processId, taskId, taskContainer);
                return false;
            }
        } else {
            logger.debug("{}/{}: can not restart task. taskService.restartInterruptedTask() operation is false", processId,
                    taskId, taskContainer);
            return false;
        }

        logger.debug("{}/{}: task container [{}] has been restarted", processId, taskId, taskContainer);
        restartedIncompleteTasksCounter.incrementAndGet();

        return true;
    }

    private boolean hasOtherNotReadyFatalTasks(UUID processId, UUID taskId, Map<UUID, Long> readyItems) {
        boolean result = false;
        if (readyItems != null) {
            for (UUID readyTaskId : readyItems.keySet()) {
                if (!taskId.equals(readyTaskId) && TransportUtils.hasFatalError(taskService.getDecisionContainer(readyTaskId,
                        processId))) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private boolean isReadyToRecover(UUID processId, UUID taskId, long startTime, String actorId, String taskList, long lastRecoveryStartTime) {

        logger.trace("#[{}]/[{}]: check if task ready to restart", processId, taskId);

        if (startTime > System.currentTimeMillis()) {//task must be started in future => skip it //recovery iterations may take some time so check current date here

            if (logger.isDebugEnabled()) {
                logger.debug("#[{}]/[{}]: must be started later at [{}]", processId, taskId, new Date(startTime));
            }

            return false;

        }

        //task is OK but it should be checked if queue is ready
        String queueName = queueService.createQueueName(actorId, taskList);
        long lastEnqueueTime = queueService.getLastPolledTaskEnqueueTime(queueName);

        // is never polled? => not ready
        if (lastEnqueueTime <= 0l) {

            logger.debug("#[{}]/[{}]: skip process restart, because queue [{}] is not polled by any actor. " +
                    "lastEnqueueTime is {}",
                    processId, taskId, queueName, lastEnqueueTime);

            return false;

        }

        // not polled since last recovery? => has no wokers
        if (lastEnqueueTime < lastRecoveryStartTime) {//still filled with old tasks => not ready

            if (logger.isDebugEnabled()) {
                logger.debug("#[{}]/[{}]: skip process restart, because queue not polled since last recovery " +
                                "activity, queue [{}] " +
                                "(last enqueue time [{}], last recovery start time [{}])",
                        processId, taskId, queueName, lastEnqueueTime, lastRecoveryStartTime);
            }

            return false;
        }

        //still filled with older tasks => this task already in queue
        if (lastEnqueueTime < startTime) {

            // todo: check decision analise time for this queueName

            if (logger.isDebugEnabled()) {
                logger.debug("#[{}]/[{}]: skip process restart, because earlier tasks in queue [{}] (last enqueue " +
                                "time [{}], last task start time [{}])",
                        processId, taskId, queueName, lastEnqueueTime, startTime);
            }

            return false;
        }

        return true;
    }


    private Collection<TaskContainer> getIncompleteTaskContainers(Map<UUID, Long> allReadyTaskIds, UUID processId) {

        Collection<TaskContainer> taskContainers = new ArrayList<>(allReadyTaskIds.size());

        Set<UUID> keys = allReadyTaskIds.keySet();
        for (UUID taskId : keys) {

            TaskContainer taskContainer = taskService.getTask(taskId, processId);

            if (taskContainer == null) {
                logger.warn("#[{}]/[{}]: not found task container in task repository", processId, taskId);
                return null;
            }

            logger.trace("#[{}]/[{}]: found not finished task container [{}]", processId, taskId, taskContainer);
            taskContainers.add(taskContainer);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("#[{}]: found [{}] not finished task containers", processId, taskContainers.size());
        }

        return taskContainers;
    }

    private boolean restartProcessFromBeginning(UUID processId) {

        if (processId == null) {
            return false;
        }

        TaskContainer startTaskContainer = processService.getStartTask(processId);

        // emulate TaskServer.startProcess()
        UUID taskId = startTaskContainer.getTaskId();
        taskService.restartTask(taskId, processId, true, false);

        dependencyService.startProcess(startTaskContainer);
        taskService.startProcess(startTaskContainer);

        logger.debug("#[{}]: restart process from start task [{}]", processId, startTaskContainer);


        boolean result = queueService.enqueueItem(startTaskContainer.getActorId(), taskId, processId,
                startTaskContainer.getStartTime(), TransportUtils.getTaskList(startTaskContainer));

        if (result) {
            recoveredProcessesCounter.incrementAndGet();
        }

        return result;
    }

    private void finishProcess(UUID processId, UUID startTaskId, Collection<UUID> finishedTaskIds) {
        // save result to process storage
        DecisionContainer decisionContainer = taskService.getDecisionContainer(startTaskId, processId);

        if (decisionContainer == null) {
            logger.error("#[{}]/[{}]: decision container for start task is null, stop finishing process");
            return;
        }

        ArgContainer argContainer = decisionContainer.getValue();
        String returnValue = argContainer.getJSONValue();
        processService.finishProcess(processId, returnValue);

        if (finishedTaskIds != null && !finishedTaskIds.isEmpty()) {
            taskService.finishProcess(processId, finishedTaskIds);
        }

        logger.debug("#[{}]: finish process. Save result [{}] from [{}] as process result", processId, returnValue, startTaskId);

        // send process to GC
        garbageCollectorService.collect(processId, timeBeforeDeleteFinishedProcess);
    }

    private void deleteTasksAndDecisions(Set<UUID> taskIds, UUID processId) {
        taskDao.deleteDecisions(taskIds, processId);
        taskDao.deleteTasks(taskIds, processId);
        for (UUID id : taskIds) {
            interruptedTasksService.delete(processId, id);
        }
    }

    public void setDependencyService(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    public void setProcessService(ProcessService processService) {
        this.processService = processService;
    }

    public void setInterruptedTasksService(InterruptedTasksService interruptedTasksService) {
        this.interruptedTasksService = interruptedTasksService;
    }
}
