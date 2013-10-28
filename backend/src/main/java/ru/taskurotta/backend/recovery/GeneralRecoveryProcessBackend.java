package ru.taskurotta.backend.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.transport.model.ActorSchedulingOptionsContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 21.10.13
 * Time: 18:24
 */
public class GeneralRecoveryProcessBackend implements RecoveryProcessBackend {

    private static final Logger logger = LoggerFactory.getLogger(GeneralRecoveryProcessBackend.class);

    private QueueBackendStatistics queueBackendStatistics;
    private DependencyBackend dependencyBackend;
    private TaskDao taskDao;
    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    // time out between recovery process in milliseconds
    private long recoveryProcessTimeOut = 60000l;

    public GeneralRecoveryProcessBackend() {}

    public GeneralRecoveryProcessBackend(QueueBackendStatistics queueBackendStatistics, DependencyBackend dependencyBackend, TaskDao taskDao, ProcessBackend processBackend, TaskBackend taskBackend, long recoveryProcessTimeOut) {
        this.queueBackendStatistics = queueBackendStatistics;
        this.dependencyBackend = dependencyBackend;
        this.taskDao = taskDao;
        this.processBackend = processBackend;
        this.taskBackend = taskBackend;
        this.recoveryProcessTimeOut = recoveryProcessTimeOut;
    }

    @Override
    public boolean restartProcess(final UUID processId) {
        logger.info("Try to recovery process [{}]", processId);

        Graph graph = dependencyBackend.getGraph(processId);
        if (graph == null) {
            logger.warn("For process [{}] not found graph, restart process", processId);

            return restartProcessFromBeginning(processId);
        }

        long lastChange = Math.max(graph.getLastApplyTimeMillis(), graph.getTouchTimeMillis());
        long changeTimeout = System.currentTimeMillis() - lastChange;
        logger.debug("For process [{}] change timeout = [{}]", processId, changeTimeout);

        if ((changeTimeout) < recoveryProcessTimeOut) {
            logger.info("Graph for process [{}] recently apply or recovery, skip recovery", processId);

            return false;
        }

        Collection<TaskContainer> taskContainers = findIncompleteTaskContainers(graph);
        if (taskContainers == null) {
            logger.warn("For process [{}] not found task containers, restart process", processId);

            return restartProcessFromBeginning(processId);
        }

        if (taskContainers.isEmpty()) {
            logger.warn("For process [{}] not found not finished tasks, replay process", processId);

            taskContainers = replayProcess(processBackend.getStartTask(processId));

            // ToDo (stukushin): if after replay process taskContainers is empty, than finish process
        }

        boolean result;
        if (taskContainers == null || taskContainers.isEmpty()) {
            result = false;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("For process [{}] try to restart [{}] tasks", processId, taskContainers.size());
            }
            result = restartTasks(taskContainers);
        }

        logger.trace("For process [{}] try to update graph", processId);
        dependencyBackend.changeGraph(new GraphDao.Updater() {
            @Override
            public UUID getProcessId() {
                return processId;
            }

            @Override
            public boolean apply(Graph graph) {
                graph.setTouchTimeMillis(System.currentTimeMillis());
                if (logger.isDebugEnabled()) {
                    logger.debug("For process [{}] update touch time to [{} ({})]", processId, graph.getTouchTimeMillis(), new Date(graph.getTouchTimeMillis()));
                }

                return true;
            }
        });

        logger.info("Process [{}] complete restart with result [{}]", processId, result);

        return result;
    }

    @Override
    public Collection<UUID> restartBrokenGroup(Collection<UUID> processIds) {

        Set<UUID> successfullyRestartedProcesses = new TreeSet<>();

        for (UUID processId : processIds) {
            if (restartProcess(processId)) {
                successfullyRestartedProcesses.add(processId);
            }
        }

        return successfullyRestartedProcesses;
    }

    private boolean restartTasks(Collection<TaskContainer> taskContainers) {

        if (taskContainers == null || taskContainers.isEmpty()) {
            logger.warn("Collection task containers for restart in null or is empty");
            return false;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Try to restart [{}] task containers", taskContainers);
        }

        UUID processId = null;
        boolean result = true;

        for (TaskContainer taskContainer : taskContainers) {

            String actorId = taskContainer.getActorId();
            processId = taskContainer.getProcessId();
            UUID taskId = taskContainer.getTaskId();

            String taskList = null;
            TaskOptionsContainer taskOptionsContainer = taskContainer.getOptions();
            if (taskOptionsContainer != null) {
                ActorSchedulingOptionsContainer actorSchedulingOptionsContainer = taskOptionsContainer.getActorSchedulingOptions();
                if (actorSchedulingOptionsContainer != null) {
                    taskList = actorSchedulingOptionsContainer.getTaskList();
                }
            }

            long now = System.currentTimeMillis();
            if (taskContainer.getStartTime() > now) {
                // this task must start in future, ignore it

                logger.trace("Task [{}] from process [{}] must started later at [{}], but now is [{}]", taskId, processId, new Date(taskContainer.getStartTime()), new Date(now));

                continue;
            }

            String queueName = queueBackendStatistics.createQueueName(taskContainer.getActorId(), taskList);
            Long lastEnqueueTime = queueBackendStatistics.getLastPolledTaskEnqueueTime(queueName);
            if (lastEnqueueTime < taskContainer.getStartTime()) {
                // this task must start later than last task pushed to queue

                logger.trace("Skip restart task [{}] for process [{}], because early tasks in queue isn't polled", taskId, processId, queueName);

                continue;
            }

            result = result & queueBackendStatistics.enqueueItem(actorId, taskId, processId, taskContainer.getStartTime(), taskList);

            logger.debug("For process [{}] add task container [{}] to queue backend", processId, taskContainer);
        }

        logger.info("For process [{}] complete restart [{}] tasks", processId, taskContainers.size());

        return result;
    }

    private Collection<TaskContainer> findIncompleteTaskContainers(Graph graph) {

        if (graph == null) {
            return null;
        }

        UUID processId = graph.getGraphId();

        logger.trace("For process [{}] try to find incomplete tasks", processId);

        Map<UUID, Long> notFinishedItems = graph.getNotFinishedItems();
        if (logger.isDebugEnabled()) {
            logger.debug("For process [{}] found [{}] not finished taskIds", processId, notFinishedItems.size());
        }

        Collection<TaskContainer> taskContainers = new ArrayList<>(notFinishedItems.size());
        Set<UUID> keys = notFinishedItems.keySet();
        for (UUID taskId : keys) {

            TaskContainer taskContainer = taskDao.getTask(taskId, processId);

            if (taskContainer == null) {
                logger.warn("For process [{}] not found task container [{}] in task repository", processId, taskId);

                return null;
            }

            logger.trace("For process [{}] found not finished task container [{}]", processId, taskContainer);
            taskContainers.add(taskContainer);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("For process [{}] found [{}] not finished task containers", processId, taskContainers.size());
        }

        return taskContainers;
    }

    private boolean restartProcessFromBeginning(UUID processId) {

        if (processId == null) {
            return false;
        }

        TaskContainer startTaskContainer = processBackend.getStartTask(processId);
        logger.debug("For process [{}] get start task [{}]", processId, startTaskContainer);

        // emulate TaskServer.startProcess()
        taskDao.addTask(startTaskContainer);
        dependencyBackend.startProcess(startTaskContainer);

        boolean result = restartTasks(Arrays.asList(startTaskContainer));

        logger.info("Restart process [{}] from start task [{}]", processId, startTaskContainer);

        return result;
    }

    private Collection<TaskContainer> replayProcess(final TaskContainer taskContainer) {

        if (taskContainer == null) {
            return null;
        }

        UUID processId = taskContainer.getProcessId();
        UUID taskId = taskContainer.getTaskId();

        logger.trace("For process [{}] try to replay task [{}]", processId, taskContainer);

        DecisionContainer decisionContainer = taskBackend.getDecision(taskId, processId);
        logger.trace("For process [{}], task [{}] get decision container [{}]", processId, taskId, decisionContainer);

        if (decisionContainer == null) {
            return new ArrayList<TaskContainer>() {{
                add(taskContainer);
            }};
        }

        TaskContainer[] arrTaskContainers = decisionContainer.getTasks();
        if (logger.isTraceEnabled()) {
            logger.trace("For process [{}], decision [{}] get new [{}] tasks", processId, taskId, arrTaskContainers.length);
        }

        Collection<TaskContainer> taskContainers = new ArrayList<>();
        for (TaskContainer tc : arrTaskContainers) {
            taskContainers.addAll(replayProcess(tc));
        }

        logger.info("For process [{}] finish replay. For task [{}] found [{}] child tasks", processId, taskId, taskContainers.size());

        return taskContainers;
    }

    public void setQueueBackendStatistics(QueueBackendStatistics queueBackendStatistics) {
        this.queueBackendStatistics = queueBackendStatistics;
    }

    public void setDependencyBackend(DependencyBackend dependencyBackend) {
        this.dependencyBackend = dependencyBackend;
    }

    public void setTaskDao(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public void setProcessBackend(ProcessBackend processBackend) {
        this.processBackend = processBackend;
    }

    public void setTaskBackend(TaskBackend taskBackend) {
        this.taskBackend = taskBackend;
    }

    public void setRecoveryProcessTimeOut(long recoveryProcessTimeOut) {
        this.recoveryProcessTimeOut = recoveryProcessTimeOut;
    }
}
