package ru.taskurotta.backend.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.queue.QueueBackend;
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
import java.util.UUID;

/**
 * User: stukushin
 * Date: 21.10.13
 * Time: 18:24
 */
public class GeneralRecoveryProcessBackend implements RecoveryProcessBackend {

    private static final Logger logger = LoggerFactory.getLogger(GeneralRecoveryProcessBackend.class);

    private QueueBackend queueBackend;
    private QueueBackendStatistics queueBackendStatistics;
    private DependencyBackend dependencyBackend;
    private TaskDao taskDao;
    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    // time out between recovery process in milliseconds
    private long recoveryProcessTimeOut = 60000l;

    public GeneralRecoveryProcessBackend() {}

    public GeneralRecoveryProcessBackend(QueueBackend queueBackend, QueueBackendStatistics queueBackendStatistics, DependencyBackend dependencyBackend, TaskDao taskDao, ProcessBackend processBackend, TaskBackend taskBackend, long recoveryProcessTimeOut) {
        this.queueBackend = queueBackend;
        this.queueBackendStatistics = queueBackendStatistics;
        this.dependencyBackend = dependencyBackend;
        this.taskDao = taskDao;
        this.processBackend = processBackend;
        this.taskBackend = taskBackend;
        this.recoveryProcessTimeOut = recoveryProcessTimeOut;
    }

    @Override
    public void restartProcess(final UUID processId) {
        logger.debug("Try to recovery process [{}]", processId);

        Graph graph = dependencyBackend.getGraph(processId);
        if (graph == null) {
            logger.warn("For process [{}] not found graph, restart process", processId);

            restartProcessFromBeginning(processId);

            return;
        }

        long lastChange = Math.max(graph.getLastApplyTimeMillis(), graph.getTouchTimeMillis());
        if (logger.isDebugEnabled()) {
            logger.debug("Graph for process [{}] last changes at [{} ({})]", processId, lastChange, new Date(lastChange));
        }

        long changeTimeout = System.currentTimeMillis() - lastChange;
        logger.debug("For process [{}] change timeout = [{}]", processId, changeTimeout);

        if ((changeTimeout) < recoveryProcessTimeOut) {
            logger.debug("Graph for process [{}] recently apply or recovery, skip recovery", processId);

            return;
        }

        Collection<TaskContainer> taskContainers = findIncompleteTaskContainers(graph);
        if (taskContainers == null) {
            logger.warn("For process [{}] not found task containers, restart process", processId);

            restartProcessFromBeginning(processId);

            return;
        }

        if (taskContainers.isEmpty()) {
            logger.warn("For process [{}] not found not finished tasks, replay process", processId);

            taskContainers = replayProcess(processBackend.getStartTask(processId));

            // ToDo (stukushin): if after replay process taskContainers is empty, than finish process
        }

        restartTasks(taskContainers);

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

        logger.info("Process [{}] complete restart", processId);
    }

    private void restartTasks(Collection<TaskContainer> taskContainers) {
        for (TaskContainer taskContainer : taskContainers) {

            String actorId = taskContainer.getActorId();

            String taskList = null;
            TaskOptionsContainer taskOptionsContainer = taskContainer.getOptions();
            if (taskOptionsContainer != null) {
                ActorSchedulingOptionsContainer actorSchedulingOptionsContainer = taskOptionsContainer.getActorSchedulingOptions();
                if (actorSchedulingOptionsContainer != null) {
                    taskList = actorSchedulingOptionsContainer.getTaskList();
                }
            }

            if (queueBackend.isTaskInQueue(actorId, taskList, taskContainer.getTaskId(), taskContainer.getProcessId())) {
                // task already in queue, never recovery
                continue;
            }

            if (taskContainer.getStartTime() > System.currentTimeMillis()) {
                // this task must start in future, ignore it
                continue;
            }

            String queueName = queueBackend.createQueueName(taskContainer.getActorId(), taskList);
            Long lastEnqueueTime = queueBackendStatistics.getLastPolledTaskEnqueueTime(queueName);
            if (lastEnqueueTime < taskContainer.getStartTime()) {
                // this task must start later than last task pushed to queue
                continue;
            }

            queueBackend.enqueueItem(taskContainer.getActorId(), taskContainer.getTaskId(), taskContainer.getProcessId(), taskContainer.getStartTime(), taskList);

            if (logger.isTraceEnabled()) {
                logger.trace("For process [{}] add task container [{}] to queue backend", taskContainer.getProcessId(), taskContainer);
            }
        }
    }

    private Collection<TaskContainer> findIncompleteTaskContainers(Graph graph) {

        UUID processId = graph.getGraphId();

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

    private void restartProcessFromBeginning(UUID processId) {
        TaskContainer startTaskContainer = processBackend.getStartTask(processId);
        logger.debug("For process [{}] get start task [{}]", processId, startTaskContainer);

        // emulate TaskServer.startProcess()
        taskDao.addTask(startTaskContainer);
        dependencyBackend.startProcess(startTaskContainer);

        restartTasks(Arrays.asList(startTaskContainer));

        logger.info("Restart process [{}] from start task [{}]", processId, startTaskContainer);
    }

    private Collection<TaskContainer> replayProcess(final TaskContainer taskContainer) {
        if (logger.isTraceEnabled()) {
            logger.trace("For process [{}] try to replay task [{}]", taskContainer.getProcessId(), taskContainer);
        }

        DecisionContainer decisionContainer = taskBackend.getDecision(taskContainer.getTaskId(), taskContainer.getProcessId());
        if (logger.isTraceEnabled()) {
            logger.trace("For process [{}], task [{}] get decision container [{}]", taskContainer.getProcessId(), taskContainer.getTaskId(), decisionContainer);
        }

        if (decisionContainer == null) {
            return new ArrayList<TaskContainer>() {{
                add(taskContainer);
            }};
        }

        TaskContainer[] arrTaskContainers = decisionContainer.getTasks();
        if (logger.isTraceEnabled()) {
            logger.trace("For process [{}], decision [{}] get new [{}] tasks", taskContainer.getProcessId(), decisionContainer.getTaskId(), arrTaskContainers.length);
        }

        Collection<TaskContainer> taskContainers = new ArrayList<>();
        for (TaskContainer tc : arrTaskContainers) {
            taskContainers.addAll(replayProcess(tc));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("For process [{}], task [{}] found [{}] child tasks", taskContainer.getProcessId(), taskContainer.getTaskId(), taskContainers.size());
        }

        return taskContainers;
    }

    public void setQueueBackend(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
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
