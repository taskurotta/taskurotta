package ru.taskurotta.service.recovery.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.recovery.RecoveryService;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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

    public static AtomicInteger recoveredFromStartProcesses = new AtomicInteger();
    public static AtomicInteger restartedTasksCounter = new AtomicInteger();
    public static AtomicInteger resurrectedTasksCounter = new AtomicInteger();

    private QueueService queueService;
    private DependencyService dependencyService;
    private ProcessService processService;
    private TaskService taskService;
    private InterruptedTasksService interruptedTasksService;
    private GarbageCollectorService garbageCollectorService;
    // time out between recovery process in milliseconds
    private long recoveryProcessChangeTimeout;
    private long findIncompleteProcessPeriod;

    public RecoveryServiceImpl(QueueService queueService, DependencyService dependencyService,
                               ProcessService processService, TaskService taskService, InterruptedTasksService interruptedTasksService,
                               GarbageCollectorService garbageCollectorService, long recoveryProcessChangeTimeout,
                               long findIncompleteProcessPeriod) {
        this.queueService = queueService;
        this.dependencyService = dependencyService;
        this.processService = processService;
        this.taskService = taskService;
        this.interruptedTasksService = interruptedTasksService;
        this.garbageCollectorService = garbageCollectorService;
        this.recoveryProcessChangeTimeout = recoveryProcessChangeTimeout;
        this.findIncompleteProcessPeriod = findIncompleteProcessPeriod;
    }

    @Override
    //returns true if some tasks have been placed to queue
    public boolean recoverProcess(final UUID processId) {
        logger.trace("Try to restart process id[{}]", processId);

        boolean result = false;

        Process process = processService.getProcess(processId);

        if (process.getState() == Process.BROKEN && restartBrokenTasks(processId)) {// check Broken process
            result = true;

        } else {
            Graph graph = dependencyService.getGraph(processId);

            if (graph == null) {// have only process service info => restart whole process
                logger.warn("#[{}]: graph was not found (possible data loss?), try to restart process from start task", processId);
                result = restartProcessFromBeginning(processId);

            } else if (graph.isFinished() && (process.getState() != Process.FINISH)) {// process is already finished => just mark process as finished
                logger.debug("#[{}]: isn't finished, but graph is finished, force finish process", processId);
                TaskContainer startTaskContainer = processService.getStartTask(processId);
                finishProcess(processId, startTaskContainer.getTaskId(), graph.getProcessTasks());

            } else if (hasRecentActivity(graph)) {// was restarted or updated recently  => leave it alone for now

                logger.debug("#[{}]: graph was recently applied or recovered, skip it", processId);

            } else {
                findAndRestartTasks(graph, processId);
            }
        }

        return result;
    }

    private boolean findAndRestartTasks(Graph graph, final UUID processId) {
        boolean result = false;

        final Collection<TaskContainer> taskContainers = findIncompleteTaskContainers(graph);
        if (taskContainers == null) { // there is a problem in task store => restart process

            logger.warn("#[{}]: task containers were not found (possible data loss?), try to restart process from start task", processId);
            result = restartProcessFromBeginning(processId);

        } else {// restart unfinished tasks

            final boolean[] boolContainer = new boolean[1];

            boolean graphUpdated = dependencyService.changeGraph(new GraphDao.Updater() {
                @Override
                public UUID getProcessId() {
                    return processId;
                }

                @Override
                public boolean apply(Graph graph) {
                    graph.setTouchTimeMillis(System.currentTimeMillis());

                    logger.debug("#[{}]: update touch time to [{} ({})]", processId, graph.getTouchTimeMillis());

                    int restartResult = restartProcessTasks(taskContainers, processId);
                    restartedTasksCounter.addAndGet(restartResult);

                    boolContainer[0] = restartResult > 0;

                    return true;
                }
            });

            result = boolContainer[0];

            logger.debug("#[{}]: has been recovered, graph update result [{}]", processId, graphUpdated);

        }
        return result;
    }

    public boolean restartBrokenTasks(final UUID processId) {

        boolean result = false;

        Graph graph = dependencyService.getGraph(processId);

        final Map<UUID, Long> allReadyTaskIds = getAllReadyTaskIds(graph, true);

        if (logger.isDebugEnabled()) {
            logger.debug("restartBrokenTasks({}) getAllReadyTaskIds.size() = {}", processId, allReadyTaskIds.size());
        }

        for (Map.Entry<UUID, Long> entry : allReadyTaskIds.entrySet()) {
            result = result || recoverTask(processId, entry.getKey());
        }

        if (result) {
            // todo: process can receive new broken tasks before this point
            processService.markProcessAsStarted(processId);
        }

        return result;
    }

    @Override
    public boolean recoverTask(final UUID processId, final UUID taskId) {
        final boolean[] boolContainer = new boolean[1];
        dependencyService.changeGraph(new GraphDao.Updater() {//acquires lock on graph

            @Override
            public UUID getProcessId() {
                return processId;
            }

            @Override
            public boolean apply(Graph graph) {
                boolean result = false;
//                long touchTime = System.currentTimeMillis();
//                graph.setTouchTimeMillis(touchTime);
                if (taskService.restartTask(taskId, processId, System.currentTimeMillis(), true)) {
                    TaskContainer tc = taskService.getTask(taskId, processId);
                    if (tc != null && queueService.enqueueItem(tc.getActorId(), tc.getTaskId(), tc.getProcessId(), System.currentTimeMillis(), TransportUtils.getTaskList(tc))) {
                        interruptedTasksService.delete(processId, taskId);
                        result = true;
                        resurrectedTasksCounter.incrementAndGet();

                        // --process state change part--
                        if (hasOtherNotReadyFatalTasks(processId, taskId, graph.getAllReadyItems())) {
                            processService.markProcessAsBroken(processId);
                        } else {
                            processService.markProcessAsStarted(processId);
                        }
                        // --/process state change part--

                    }
                }

                boolContainer[0] = result;

                logger.debug("ProcessId [{}] graph change applied, result[{}]", processId, result);
                return result;
            }

        });

        return boolContainer[0];
    }

    private boolean hasRecentActivity(Graph graph) {

        if (graph == null) {
            return false;
        }

        boolean result = false;

        long lastChange = Math.max(graph.getLastApplyTimeMillis(), graph.getTouchTimeMillis());
        if (lastChange > 0) {//has some modifications, check if they expired

            long changeTimeout = System.currentTimeMillis() - lastChange;
            logger.debug("#[{}]: activity check for graph: change timeout[{}], last change[{}]", graph.getGraphId(), changeTimeout, lastChange);

            //todo: may be we not need "recoveryProcessChangeTimeout" property?
            //todo: we can have two properties: process-finish-timeout and process-idle-timeout.
            //todo: And have different recovery strategies for each other.
            //todo: to find processes of process-idle-timeout we can use mongodb Graph collection
            result = changeTimeout < recoveryProcessChangeTimeout;
        }

        return result;
    }

    @Override
    public Collection<UUID> recoverProcesses(Collection<UUID> processIds) {
        Collection<UUID> successfullyRestartedProcesses = new ArrayList<>();

        for (UUID processId : processIds) {
            if (recoverProcess(processId)) {
                successfullyRestartedProcesses.add(processId);
            }
        }

        return successfullyRestartedProcesses;
    }

    private int restartProcessTasks(Collection<TaskContainer> taskContainers, UUID processId) {
        logger.trace("#[{}]: try to restart [{}] task containers", processId, taskContainers);

        int restartedTasks = 0;

        if (taskContainers != null && !taskContainers.isEmpty()) {

            long lastRecoveryStartTime = System.currentTimeMillis() - findIncompleteProcessPeriod;

            // check tasks
            for (Iterator<TaskContainer> it = taskContainers.iterator(); it.hasNext(); ) {
                TaskContainer taskContainer = it.next();
                UUID taskId = taskContainer.getTaskId();
                long startTime = taskContainer.getStartTime();
                String taskList = TransportUtils.getTaskList(taskContainer);
                String actorId = taskContainer.getActorId();

                if (TransportUtils.hasFatalError(taskService.getDecision(taskId, processId))) {// process is broken now. Skip it.
                    taskContainers = null;
                    break;

                } else if (!isReadyToRecover(processId, taskId, startTime, actorId, taskList, lastRecoveryStartTime)) {
                    // remove not ready task from collection
                    it.remove();
                    continue;

                } else if (!hasAllPromisesInitialized(taskId, processId)) {
                    if (!restartProcessFromBeginning(processId)) {
                        logger.error("Can not restart process from beginning. Process has ready task without " +
                                "consistent arguments. Process id = {} task id = {}", processId, taskId);
                    }
                    taskContainers = null;// drop collection of tasks, cause its process has been restarted.
                    break;
                }

            }

            if (taskContainers != null) {

                // restart tasks
                for (TaskContainer taskContainer : taskContainers) {
                    UUID taskId = taskContainer.getTaskId();
                    long startTime = taskContainer.getStartTime();
                    String taskList = TransportUtils.getTaskList(taskContainer);
                    String actorId = taskContainer.getActorId();

                    if (taskService.restartTask(taskId, processId, System.currentTimeMillis(), false)) {
                        if (queueService.enqueueItem(actorId, taskId, processId, startTime, taskList)) {
                            restartedTasks++;
                        } else {
                            logger.debug("#[{}]/[{}]: can not restart task. enqueue operation is false", processId,
                                    taskId, taskContainer);
                        }
                    } else {
                        logger.debug("#[{}]/[{}]: can not restart task. taskService.restartTask() operation is false",
                                processId, taskId, taskContainer);
                    }
                }
            }

        }

        logger.debug("#[{}]: complete restart of [{}] tasks", processId, restartedTasks);

        return restartedTasks;
    }

    private boolean hasAllPromisesInitialized(UUID taskId, UUID processId) {
        try {
            return taskService.getTaskToExecute(taskId, processId, true) != null;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    private boolean isReadyToRecover(UUID processId, UUID taskId, long startTime, String actorId, String taskList, long lastRecoveryStartTime) {

        logger.trace("#[{}]/[{}]: check if task ready to restart", processId, taskId);

        boolean result = true;//consider every task as ready by default

        if (startTime > System.currentTimeMillis()) {//task must be started in future => skip it //recovery iterations may take some time so check current date here

            if (logger.isDebugEnabled()) {
                logger.debug("#[{}]/[{}]: must be started later at [{}]", processId, taskId, new Date(startTime));
            }

            result = false;

        } else {
            //task is OK but it should be checked if queue is ready
            String queueName = queueService.createQueueName(actorId, taskList);
            long lastEnqueueTime = queueService.getLastPolledTaskEnqueueTime(queueName);

            if (lastEnqueueTime <= 0l) {// is never polled? => not ready
                result = false;

                logger.debug("#[{}]/[{}]: skip process restart, because queue [{}] is not polled by any actor", processId, taskId, queueName);

            } else if (lastEnqueueTime < lastRecoveryStartTime) {// not polled since last recovery? => has no wokers
                result = false;

                logger.debug("#[{}]/[{}]: skip process restart, because queue not polled since last recovery " +
                                "activity, queue [{}] " +
                                "(last enqueue time [{}], last recovery start time [{}])",
                        processId, taskId, queueName, lastEnqueueTime, lastRecoveryStartTime);

            } else if (lastEnqueueTime < startTime) {//still filled with older tasks => this task already in queue
                // todo: check decision analysis time for this queueName
                result = false;

                logger.debug("#[{}]/[{}]: skip process restart, because earlier tasks in queue [{}] (last enqueue " +
                                "time [{}], last task start time [{}])",
                        processId, taskId, queueName, lastEnqueueTime, startTime);

            }
        }

        return result;
    }

    private Map<UUID, Long> getAllReadyTaskIds(final Graph graph, final boolean touchGraph) {

        final Map<UUID, Long> allReadyTaskIds = new HashMap<>();

        dependencyService.changeGraph(new GraphDao.Updater() {
            @Override
            public UUID getProcessId() {
                return graph.getGraphId();
            }

            @Override
            public boolean apply(Graph graph) {

                allReadyTaskIds.putAll(graph.getAllReadyItems());

                if (touchGraph) {
                    graph.setTouchTimeMillis(System.currentTimeMillis());
                    return true;
                }

                return false;
            }
        });

        return allReadyTaskIds;
    }

    private Collection<TaskContainer> findIncompleteTaskContainers(Graph graph) {

        if (graph == null) {
            return null;
        }

        final UUID processId = graph.getGraphId();

        logger.trace("#[{}]: try to find incomplete tasks", processId);

        final Map<UUID, Long> allReadyTaskIds = getAllReadyTaskIds(graph, false);

        if (logger.isDebugEnabled()) {
            logger.debug("#[{}]: found [{}] not finished taskIds", processId, allReadyTaskIds.size());
        }

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
        taskService.restartTask(taskId, processId, System.currentTimeMillis(), true);

        dependencyService.startProcess(startTaskContainer);
        taskService.startProcess(startTaskContainer);

        logger.debug("#[{}]: restart process from start task [{}]", processId, startTaskContainer);


        boolean result = queueService.enqueueItem(startTaskContainer.getActorId(), taskId, processId,
                startTaskContainer.getStartTime(), TransportUtils.getTaskList(startTaskContainer));

        if (result) {
            recoveredFromStartProcesses.incrementAndGet();
        }

        return result;
    }

    private void finishProcess(UUID processId, UUID startTaskId, Collection<UUID> finishedTaskIds) {
        // save result to process storage
        DecisionContainer decisionContainer = taskService.getDecision(startTaskId, processId);

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
        garbageCollectorService.collect(processId);
    }

    private boolean hasOtherNotReadyFatalTasks(UUID processId, UUID taskId, Map<UUID, Long> readyItems) {
        boolean result = false;
        if (readyItems != null) {
            for (UUID readyTaskId : readyItems.keySet()) {
                if (!taskId.equals(readyTaskId) && TransportUtils.hasFatalError(taskService.getDecision(readyTaskId, processId))) {
                    result = true;
                    break;
                }
            }
        }
        return result;
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
