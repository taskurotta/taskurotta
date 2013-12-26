package ru.taskurotta.service.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.BrokenProcessService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskDao;
import ru.taskurotta.service.storage.TaskService;
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
public class GeneralRecoveryProcessService implements RecoveryProcessService {

    private static final Logger logger = LoggerFactory.getLogger(GeneralRecoveryProcessService.class);

    private QueueService queueService;
    private DependencyService dependencyService;
    private TaskDao taskDao;
    private ProcessService processService;
    private TaskService taskService;
    private BrokenProcessService brokenProcessService;
    // time out between recovery process in milliseconds
    private long recoveryProcessTimeOut;

    public GeneralRecoveryProcessService() {}

    public GeneralRecoveryProcessService(QueueService queueService, DependencyService dependencyService, TaskDao taskDao, ProcessService processService, TaskService taskService, BrokenProcessService brokenProcessService, long recoveryProcessTimeOut) {
        this.queueService = queueService;
        this.dependencyService = dependencyService;
        this.taskDao = taskDao;
        this.processService = processService;
        this.taskService = taskService;
        this.brokenProcessService = brokenProcessService;
        this.recoveryProcessTimeOut = recoveryProcessTimeOut;
    }

    @Override
    public boolean restartProcess(final UUID processId) {
        logger.trace("Try to recovery process [{}]", processId);

        Graph graph = dependencyService.getGraph(processId);
        if (graph == null) {
            logger.warn("#[{}]: not found graph, restart process", processId);
            return restartProcessFromBeginning(processId);
        }

        long lastChange = Math.max(graph.getLastApplyTimeMillis(), graph.getTouchTimeMillis());
        long changeTimeout = System.currentTimeMillis() - lastChange;
        logger.debug("#[{}]: change timeout = [{}]", processId, changeTimeout);

        if (changeTimeout < recoveryProcessTimeOut) {
            logger.debug("#[{}]: graph recently apply or recovery, skip recovery", processId);
            return false;
        }

        Collection<TaskContainer> taskContainers = findIncompleteTaskContainers(graph);
        if (taskContainers == null) {
            logger.warn("#[{}]: not found task containers, restart process", processId);
            return restartProcessFromBeginning(processId);
        }

        if (taskContainers.isEmpty()) {
            logger.warn("#[{}]: not found not finished tasks, replay process", processId);
            TaskContainer startTaskContainer = processService.getStartTask(processId);
            taskContainers = replayProcess(startTaskContainer);
            if (logger.isDebugEnabled()) {
                logger.debug("#[{}]: finish replay. For task [{}] found [{}] child tasks", processId, startTaskContainer.getTaskId(), taskContainers.size());
            }
        }

        boolean result;
        if (taskContainers == null || taskContainers.isEmpty()) {
            // ToDo (stukushin): if after replay process taskContainers is empty, than finish process
            result = false;
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("#[{}]: try to restart [{}] tasks", processId, taskContainers.size());
            }
            result = restartTasks(taskContainers);
        }

        logger.trace("#[{}]: try to update graph", processId);
        dependencyService.changeGraph(new GraphDao.Updater() {
            @Override
            public UUID getProcessId() {
                return processId;
            }

            @Override
            public boolean apply(Graph graph) {
                graph.setTouchTimeMillis(System.currentTimeMillis());
                if (logger.isTraceEnabled()) {
                    logger.trace("#[{}]: update touch time to [{} ({})]", processId, graph.getTouchTimeMillis(), new Date(graph.getTouchTimeMillis()));
                }

                return true;
            }
        });

        logger.info("#[{}]: complete restart with result [{}]", processId, result);

        if (result) {
            brokenProcessService.delete(processId);
        }

        return result;
    }

    @Override
    public Collection<UUID> restartProcessCollection(Collection<UUID> processIds) {

        Set<UUID> successfullyRestartedProcesses = new TreeSet<>();

        for (UUID processId : processIds) {
            if (restartProcess(processId)) {
                successfullyRestartedProcesses.add(processId);
            }
        }

        brokenProcessService.deleteCollection(successfullyRestartedProcesses);

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
        int restartedTasks = 0;

        for (TaskContainer taskContainer : taskContainers) {

            String actorId = taskContainer.getActorId();
            processId = taskContainer.getProcessId();
            UUID taskId = taskContainer.getTaskId();
            long startTime = taskContainer.getStartTime();

            logger.trace("#[{}]/[{}]: try to restart", processId, taskId);

            String taskList = null;
            TaskOptionsContainer taskOptionsContainer = taskContainer.getOptions();
            if (taskOptionsContainer != null) {
                ActorSchedulingOptionsContainer actorSchedulingOptionsContainer = taskOptionsContainer.getActorSchedulingOptions();
                if (actorSchedulingOptionsContainer != null) {
                    taskList = actorSchedulingOptionsContainer.getTaskList();
                }
            }

            long now = System.currentTimeMillis();
            if (startTime > now) {
                // this task must start in future, ignore it

                if (logger.isDebugEnabled()) {
                    logger.debug("#[{}]/[{}]: must started later at [{}], but now is [{}]", processId, taskId, new Date(startTime), new Date(now));
                }

                continue;
            }

            String queueName = queueService.createQueueName(taskContainer.getActorId(), taskList);
            Long lastEnqueueTime = queueService.getLastPolledTaskEnqueueTime(queueName);
            if (logger.isTraceEnabled()) {
                logger.trace("#[{}]/[{}]: startTime = [{}], queue [{}] last enqueue time = [{}]", processId, taskId, new Date(startTime), queueName, new Date(lastEnqueueTime));
            }

//            if (lastEnqueueTime > 0 && lastEnqueueTime < taskContainer.getStartTime()) {
            if (lastEnqueueTime < taskContainer.getStartTime()) {
                // this task must start later than last task pushed to queue

                if (logger.isDebugEnabled()) {
                    logger.debug("#[{}]/[{}]: (startTime = [{}]) skip restart, because early tasks in queue [{}] (last enqueue time = [{}]) isn't polled.", processId, taskId, new Date(startTime), queueName, new Date(lastEnqueueTime));
                }

                continue;
            }

            result = result & queueService.enqueueItem(actorId, taskId, processId, taskContainer.getStartTime(), taskList);

            logger.debug("#[{}]/[{}]: add task container [{}] to queue service", processId, taskId, taskContainer);

            restartedTasks++;
        }

        logger.debug("#[{}]: complete restart [{}] tasks", processId, restartedTasks);

        return result;
    }

    private Collection<TaskContainer> findIncompleteTaskContainers(Graph graph) {

        if (graph == null) {
            return null;
        }

        UUID processId = graph.getGraphId();

        logger.trace("#[{}]: try to find incomplete tasks", processId);

        Map<UUID, Long> notFinishedItems = graph.getNotFinishedItems();
        if (logger.isDebugEnabled()) {
            logger.debug("#[{}]: found [{}] not finished taskIds", processId, notFinishedItems.size());
        }

        Collection<TaskContainer> taskContainers = new ArrayList<>(notFinishedItems.size());
        Set<UUID> keys = notFinishedItems.keySet();
        for (UUID taskId : keys) {

            TaskContainer taskContainer = taskDao.getTask(taskId, processId);

            if (taskContainer == null) {
                logger.warn("#[{}]: not found task container [{}] in task repository", processId, taskId);
                return null;
            }

            logger.trace("#[{}]: found not finished task container [{}]", processId, taskContainer);
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
        logger.debug("#[{}]: get start task [{}]", processId, startTaskContainer);

        // emulate TaskServer.startProcess()
        taskDao.addTask(startTaskContainer);
        dependencyService.startProcess(startTaskContainer);

        boolean result = restartTasks(Arrays.asList(startTaskContainer));

        logger.info("#[{}]: restart from start task [{}]", processId, startTaskContainer);

        return result;
    }

    private Collection<TaskContainer> replayProcess(TaskContainer taskContainer) {

        if (taskContainer == null) {
            return null;
        }

        UUID processId = taskContainer.getProcessId();
        UUID taskId = taskContainer.getTaskId();

        logger.trace("#[{}]/[{}]: try to replay task", processId, taskId);

        DecisionContainer decisionContainer = taskService.getDecision(taskId, processId);
        logger.trace("#[{}]/[{}]: get decision container [{}]", processId, taskId, decisionContainer);

        if (decisionContainer == null) {
            return Arrays.asList(taskContainer);
        }

        TaskContainer[] arrTaskContainers = decisionContainer.getTasks();
        if (arrTaskContainers == null || arrTaskContainers.length == 0) {
            return Arrays.asList(taskContainer);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("#[{}]/[{}]: decision [{}] get new [{}] tasks", processId, taskId, decisionContainer, arrTaskContainers.length);
        }

        Collection<TaskContainer> taskContainers = new ArrayList<>();
        for (TaskContainer tc : arrTaskContainers) {
            taskContainers.addAll(replayProcess(tc));
        }

        return taskContainers;
    }

    public void setDependencyService(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    public void setTaskDao(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public void setProcessService(ProcessService processService) {
        this.processService = processService;
    }

    public void setBrokenProcessService(BrokenProcessService brokenProcessService) {
        this.brokenProcessService = brokenProcessService;
    }
}
