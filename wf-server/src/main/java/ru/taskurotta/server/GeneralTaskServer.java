package ru.taskurotta.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;
import ru.taskurotta.backend.snapshot.SnapshotService;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.model.TaskType;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:04 PM
 */
public class GeneralTaskServer implements TaskServer {

    private static final Logger logger = LoggerFactory.getLogger(GeneralTaskServer.class);

    private static final Metrics.CheckPoint chpStartProcess = StaticMetrics.create("GeneralTaskServer.startProcess");
    private static final Metrics.CheckPoint chpPoll = StaticMetrics.create("GeneralTaskServer.poll");
    private static final Metrics.CheckPoint chpPoll_empty = StaticMetrics.create("GeneralTaskServer.poll_empty");
    private static final Metrics.CheckPoint chpRelease = StaticMetrics.create("GeneralTaskServer.release");
    private static final Metrics.CheckPoint chpProcessDecision_error = StaticMetrics.create("GeneralTaskServer" +
            ".processDecision_error");
    private static final Metrics.CheckPoint chpProcessDecision_fail = StaticMetrics.create("GeneralTaskServer" +
            ".processDecision_fail");
    private static final Metrics.CheckPoint chpProcessDecision_ok = StaticMetrics.create("GeneralTaskServer" +
            ".processDecision_ok");

    protected ProcessBackend processBackend;
    protected TaskBackend taskBackend;
    protected QueueBackend queueBackend;
    protected DependencyBackend dependencyBackend;
    protected ConfigBackend configBackend;
    protected SnapshotService snapshotService;

    /*
     *  For tests ONLY
     */
    public GeneralTaskServer() {

    }

    public SnapshotService getSnapshotService() {
        return snapshotService;
    }

    public void setSnapshotService(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    public GeneralTaskServer(BackendBundle backendBundle) {
        this.processBackend = backendBundle.getProcessBackend();
        this.taskBackend = backendBundle.getTaskBackend();
        this.queueBackend = backendBundle.getQueueBackend();
        this.dependencyBackend = backendBundle.getDependencyBackend();
        this.configBackend = backendBundle.getConfigBackend();
    }

    public GeneralTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        this.processBackend = processBackend;
        this.taskBackend = taskBackend;
        this.queueBackend = queueBackend;
        this.dependencyBackend = dependencyBackend;
        this.configBackend = configBackend;
    }

    @Override
    public void startProcess(TaskContainer task) {

        long smt = System.currentTimeMillis();

        // some consistence check
        if (!task.getType().equals(TaskType.DECIDER_START)) {
            // TODO: send error to client
            throw new IllegalStateException("Can not start process. Task should be type of " + TaskType.DECIDER_START);
        }

        // registration of new process
        // atomic statement
        processBackend.startProcess(task);

        // inform taskBackend about new process
        // idempotent statement
        taskBackend.startProcess(task);

        // inform dependencyBackend about new process
        // idempotent statement
        dependencyBackend.startProcess(task);

        // we assume that new process task has no dependencies and it is ready to enqueue.
        // idempotent statement
        enqueueTask(task.getTaskId(), task.getProcessId(), task.getActorId(), task.getStartTime(), getTaskList(task));


        processBackend.startProcessCommit(task);

        chpStartProcess.mark(smt);
    }


    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        long smt = System.currentTimeMillis();

        String actorId = ActorUtils.getActorId(actorDefinition);

        if (configBackend.isActorBlocked(actorId)) {
            throw null;
        }

        // atomic statement
        TaskQueueItem tqi = queueBackend.poll(actorDefinition.getFullName(), actorDefinition.getTaskList());
        if (tqi == null) {

            chpPoll_empty.mark(smt);
            return null;
        }

        // idempotent statement
        TaskContainer taskContainer = taskBackend.getTaskToExecute(tqi.getTaskId(), tqi.getProcessId());

        queueBackend.pollCommit(actorDefinition.getFullName(), tqi.getTaskId(), tqi.getProcessId());

        chpPoll.mark(smt);

        return taskContainer;
    }


    public DependencyBackend getDependencyBackend() {
        return dependencyBackend;
    }

    @Override
    public void release(DecisionContainer taskDecision) {

        long smt = System.currentTimeMillis();

        if (configBackend.isActorBlocked(taskDecision.getActorId())) {
            return;
        }

        // save it firstly
        taskBackend.addDecision(taskDecision);
        processDecision(taskDecision);

        chpRelease.mark(smt);
    }

    /**
     * @return true if snapshot processing required, false otherwise
     */
    public void processDecision(DecisionContainer taskDecision) {

        logger.debug("Start processing task decision[{}]", taskDecision);

        long smt = System.currentTimeMillis();

        UUID taskId = taskDecision.getTaskId();
        UUID processId = taskDecision.getProcessId();


        // if Error
        if (taskDecision.containsError()) {
            // enqueue task immediately if needed
            if (taskDecision.getRestartTime() != TaskDecision.NO_RESTART) {

                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer asyncTask = taskBackend.getTask(taskId, processId);
                logger.debug("Error task enqueued again, taskId [{}]", taskId);
                enqueueTask(taskId, asyncTask.getProcessId(), asyncTask.getActorId(), taskDecision.getRestartTime(), getTaskList(asyncTask));
            }

            taskBackend.addDecisionCommit(taskDecision);

            chpProcessDecision_error.mark(smt);
            return;
        }

        // idempotent statement
        DependencyDecision dependencyDecision = dependencyBackend.applyDecision(taskDecision);

        logger.debug("release() received dependencyDecision = [{}]", dependencyDecision);

        if (dependencyDecision.isFail()) {

            logger.debug("release() failed dependencyDecision. release() should be retried after RELEASE_TIMEOUT");

            // leave release() method.
            // RELEASE_TIMEOUT should be automatically fired

            chpProcessDecision_fail.mark(smt);
            return;
        }

        List<UUID> readyTasks = dependencyDecision.getReadyTasks();

        if (readyTasks != null) {

            for (UUID taskId2Queue : readyTasks) {

                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer asyncTask = taskBackend.getTask(taskId2Queue, processId);
                enqueueTask(taskId2Queue, asyncTask.getProcessId(), asyncTask.getActorId(), asyncTask.getStartTime(), getTaskList(asyncTask));
            }

        }

        if (dependencyDecision.isProcessFinished()) {
            processBackend.finishProcess(processId,
                    dependencyDecision.getFinishedProcessValue());
            taskBackend.finishProcess(processId, dependencyBackend.getGraph(processId).getProcessTasks());
        }

        taskBackend.addDecisionCommit(taskDecision);

        processSnapshot(taskDecision, dependencyDecision);
        logger.debug("Finish processing task decision[{}]", taskId);

        chpProcessDecision_ok.mark(smt);
    }

    protected void processSnapshot(DecisionContainer taskDecision, DependencyDecision dependencyDecision) {
        logger.debug("Snapshot processing initialized with taskDecision[{}], dependencyDecision[{}]", taskDecision, dependencyDecision);
        //TODO: implement it
    }

    /**
     * Send task to the queue for processing
     *
     * @param startTime time to start delayed task. set to 0 to start it immediately
     * @param taskList  -
     */
    protected void enqueueTask(UUID taskId, UUID processId, String actorId, long startTime, String taskList) {

        queueBackend.enqueueItem(actorId, taskId, processId, startTime, taskList);

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
