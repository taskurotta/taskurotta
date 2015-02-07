package ru.taskurotta.service.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.BrokenProcessService;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: stukushin
 * Date: 21.10.13
 * Time: 18:24
 */
public class GeneralRecoveryProcessService implements RecoveryProcessService {

    private static final Logger logger = LoggerFactory.getLogger(GeneralRecoveryProcessService.class);

    public static AtomicInteger restartedProcessesCounter = new AtomicInteger();
    public static AtomicInteger restartedTasksCounter = new AtomicInteger();

    private QueueService queueService;
    private DependencyService dependencyService;
    private ProcessService processService;
    private TaskService taskService;
    private BrokenProcessService brokenProcessService;
    private GarbageCollectorService garbageCollectorService;
    // time out between recovery process in milliseconds
    private long recoveryProcessChangeTimeout;
    private long findIncompleteProcessPeriod;

    public GeneralRecoveryProcessService() {
    }

    public GeneralRecoveryProcessService(QueueService queueService, DependencyService dependencyService,
                                         ProcessService processService, TaskService taskService, BrokenProcessService brokenProcessService,
                                         GarbageCollectorService garbageCollectorService, long recoveryProcessChangeTimeout,
                                         long findIncompleteProcessPeriod) {
        this.queueService = queueService;
        this.dependencyService = dependencyService;
        this.processService = processService;
        this.taskService = taskService;
        this.brokenProcessService = brokenProcessService;
        this.garbageCollectorService = garbageCollectorService;
        this.recoveryProcessChangeTimeout = recoveryProcessChangeTimeout;
        this.findIncompleteProcessPeriod = findIncompleteProcessPeriod;
    }


    @Override
    public boolean restartProcess(final UUID processId) {
        logger.trace("#[{}]: try to restart process", processId);

        // val=true if some tasks have been placed to queue
        boolean result = false;

        Graph graph = dependencyService.getGraph(processId);


        if (graph == null) {
            // have only process service info => restart whole process

            logger.warn("#[{}]: graph was not found (possible data loss?), try to restart process from start task", processId);
            result = restartProcessFromBeginning(processId);

        } else if (graph.isFinished()) {
            // process is already finished => just mark process as finished

            logger.debug("#[{}]: isn't finished, but graph is finished, force finish process", processId);
            TaskContainer startTaskContainer = processService.getStartTask(processId);
            finishProcess(processId, startTaskContainer.getTaskId(), graph.getProcessTasks());

        } else if (hasRecentActivity(graph)) {
            // was restarted or updated recently  => leave it alone for now

            logger.debug("#[{}]: graph was recently applied or recovered, skip it", processId);

        } else {
            // require restart => try to find process's tasks for restart

            final Collection<TaskContainer> taskContainers = findIncompleteTaskContainers(graph);
            if (taskContainers == null) {
                // there is a problem in task store => restart process

                logger.warn("#[{}]: task containers were not found (possible data loss?), try to restart process from start task", processId);
                result = restartProcessFromBeginning(processId);

            } else {
                // restart unfinished tasks

                final AtomicBoolean boolContainer = new AtomicBoolean();

                logger.trace("#[{}]: try to update graph", processId);
                boolean graphUpdated = dependencyService.changeGraph(new GraphDao.Updater() {
                    @Override
                    public UUID getProcessId() {
                        return processId;
                    }

                    @Override
                    public boolean apply(Graph graph) {
                        graph.setTouchTimeMillis(System.currentTimeMillis());

                        if (logger.isTraceEnabled()) {
                            logger.trace("#[{}]: update touch time to [{} ({})]", processId, graph.getTouchTimeMillis(),
                                    new Date(graph.getTouchTimeMillis()));
                        }

                        int restartResult = restartTasks(taskContainers, processId);
                        restartedTasksCounter.addAndGet(restartResult);

                        boolContainer.set(restartResult > 0);

                        // todo: is it really needed?
                        brokenProcessService.delete(processId);

                        return true;
                    }
                });

                result = boolContainer.get();

                logger.debug("#[{}]: has been recovered, graph update result [{}]", processId, graphUpdated);

            }

        }

        return result;
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
            logger.debug("#[{}]: activity check for graph: change timeout[{}], last change[{}]", graph.getGraphId(), changeTimeout, lastChange);

            // todo: may be we not need "recoveryProcessChangeTimeout" property?
            // we can have two properties: process-finish-timeout and process-idle-timeout.
            // And have different recovery strategies for each other.
            // to find processes of process-idle-timeout we can use mongodb Graph collection
            result = changeTimeout < recoveryProcessChangeTimeout;
        }

        return result;
    }

    @Override
    public Collection<UUID> restartProcessCollection(Collection<UUID> processIds) {
        Collection<UUID> successfullyRestartedProcesses = new ArrayList<>();

        for (UUID processId : processIds) {
            if (restartProcess(processId)) {
                successfullyRestartedProcesses.add(processId);
            }
        }

        brokenProcessService.deleteCollection(successfullyRestartedProcesses);

        return successfullyRestartedProcesses;
    }

    private int restartTasks(Collection<TaskContainer> taskContainers, UUID processId) {
        logger.trace("#[{}]: try to restart [{}] task containers", processId, taskContainers);

        int restartedTasks = 0;

        if (taskContainers != null && !taskContainers.isEmpty()) {

            long lastRecoveryStartTime = System.currentTimeMillis() - findIncompleteProcessPeriod;

            for (TaskContainer taskContainer : taskContainers) {
                UUID taskId = taskContainer.getTaskId();
                long startTime = taskContainer.getStartTime();
                String taskList = TransportUtils.getTaskList(taskContainer);
                String actorId = taskContainer.getActorId();

                if (isReadyToRecover(processId, taskId, startTime, actorId, taskList, lastRecoveryStartTime)
                        && queueService.enqueueItem(actorId, taskId, processId, startTime, taskList)) {

                    logger.trace("#[{}]/[{}]: task container [{}] have been restarted", processId, taskId, taskContainer);

                    restartedTasks++;
                }
            }

        }

        logger.debug("#[{}]: complete restart of [{}] tasks", processId, restartedTasks);

        return restartedTasks;
    }

    private boolean isReadyToRecover(UUID processId, UUID taskId, long startTime, String actorId, String taskList, long lastRecoveryStartTime) {

        logger.trace("#[{}]/[{}]: check if task ready to restart", processId, taskId);

        boolean result = true;//consider every task as ready by default

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

            logger.debug("#[{}]/[{}]: skip process restart, because queue [{}] is not polled by any actor", processId, taskId, queueName);

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

            if (logger.isDebugEnabled()) {
                logger.debug("#[{}]/[{}]: skip process restart, because earlier tasks in queue [{}] (last enqueue " +
                                "time [{}], last task start time [{}])",
                        processId, taskId, queueName, lastEnqueueTime, startTime);
            }

            return false;
        }

        return result;
    }

    private Collection<TaskContainer> findIncompleteTaskContainers(Graph graph) {

        if (graph == null) {
            return null;
        }

        final UUID processId = graph.getGraphId();

        logger.trace("#[{}]: try to find incomplete tasks", processId);

        final Map<UUID, Long> notFinishedItems = new HashMap<>();

        dependencyService.changeGraph(new GraphDao.Updater() {
            @Override
            public UUID getProcessId() {
                return processId;
            }

            @Override
            public boolean apply(Graph graph) {

                // todo:should create new method on Graph with Set<UUID> return type
                // Set is enough for this needs
                notFinishedItems.putAll(graph.getAllReadyItems());

                return false;
            }
        });

        if (logger.isDebugEnabled()) {
            logger.debug("#[{}]: found [{}] not finished taskIds", processId, notFinishedItems.size());
        }

        Collection<TaskContainer> taskContainers = new ArrayList<>(notFinishedItems.size());

        Set<UUID> keys = notFinishedItems.keySet();
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
        taskService.startProcess(startTaskContainer);
        dependencyService.startProcess(startTaskContainer);

        logger.debug("#[{}]: restart process from start task [{}]", processId, startTaskContainer);

        boolean result = queueService.enqueueItem(startTaskContainer.getActorId(), startTaskContainer.getTaskId(), processId,
                startTaskContainer.getStartTime(), TransportUtils.getTaskList(startTaskContainer));

        if (result) {
            restartedProcessesCounter.incrementAndGet();
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

    public void setDependencyService(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    public void setProcessService(ProcessService processService) {
        this.processService = processService;
    }

    public void setBrokenProcessService(BrokenProcessService brokenProcessService) {
        this.brokenProcessService = brokenProcessService;
    }
}
