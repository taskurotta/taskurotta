package ru.taskurotta.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.server.hazelcast.ProcessPartitionKey;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.model.TaskType;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:04 PM
 */
public class HazelcastTaskServer implements TaskServer {

    private final static Logger logger = LoggerFactory.getLogger(HazelcastTaskServer.class);

    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;

    private final HazelcastInstance hazelcastInstance;

    private final IMap<ProcessPartitionKey, Queue<DecisionContainer>> decisionQueues;
    private static final String DECISION_QUEUE_MAP_NAME = "decisionQueueMap";

    private int coordinatorPoolSize = 10;
    public String taskServerName = null;

    public HazelcastTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend, HazelcastInstance hazelcastInstance) {
        this.processBackend = processBackend;
        this.taskBackend = taskBackend;
        this.queueBackend = queueBackend;
        this.dependencyBackend = dependencyBackend;
        this.configBackend = configBackend;
        this.hazelcastInstance = hazelcastInstance;

        try {
            taskServerName = HazelcastTaskServer.class.getName() + "-" + InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("Catch exception while get task server address", e);
            throw new RuntimeException(e);
        }

        this.decisionQueues = hazelcastInstance.getMap(DECISION_QUEUE_MAP_NAME);

        Executors.newSingleThreadExecutor().submit(new Coordinator(coordinatorPoolSize));
    }

    public HazelcastTaskServer(BackendBundle backendBundle, HazelcastInstance hazelcastInstance) {
        this(backendBundle.getProcessBackend(), backendBundle.getTaskBackend(), backendBundle.getQueueBackend(), backendBundle.getDependencyBackend(), backendBundle.getConfigBackend(), hazelcastInstance);
    }

    class Coordinator implements Callable {

        private List<Future<ProcessPartitionKey>> futures = new LinkedList<>();

        private ExecutorService executorService;

        Coordinator(int poolSize) {
            this.executorService = Executors.newFixedThreadPool(poolSize);
        }

        @Override
        public Void call() {
            logger.trace("Start Coordinator thread [{}]", Thread.currentThread().getName());

            while (true) {
                logger.trace("Decision queues size [{}]", decisionQueues.size());

                if (!decisionQueues.isEmpty()) {

                    for(ProcessPartitionKey processPartitionKey : decisionQueues.keySet()) {

                        if (decisionQueues.get(processPartitionKey).isEmpty()) {
                            continue;
                        }

                        ILock lock = hazelcastInstance.getLock(processPartitionKey);

                        if (lock.isLocked()) {
                            continue;
                        }

                        if (lock.tryLock()) {
                            logger.trace("Lock process queue id [{}]", processPartitionKey);

                            Future<ProcessPartitionKey> future = executorService.submit(new ApplyDecisionTask(processPartitionKey, decisionQueues.get(processPartitionKey).poll()));
                            futures.add(future);
                        }
                    }
                }

                logger.trace("Futures list size [{}]", futures.size());

                if (!futures.isEmpty()) {

                    ListIterator<Future<ProcessPartitionKey>> listIterator = futures.listIterator();
                    while (listIterator.hasNext()) {

                        Future<ProcessPartitionKey> future = listIterator.next();

                        if (future.isDone()) {
                            try {
                                ProcessPartitionKey processPartitionKey = future.get();

                                ILock lock = hazelcastInstance.getLock(processPartitionKey);

                                if (lock.isLocked()) {
                                    lock.unlock();
                                }

                                if (lock.isLocked()) {
                                    logger.trace("Process queue id [{}] can't unlock", processPartitionKey);
                                    continue;
                                }

                                logger.trace("Unlock process queue id [{}]", processPartitionKey);

                                listIterator.remove();
                            } catch (InterruptedException | ExecutionException e) {
                                logger.error("Catch exception while get future [" + future + "] value", e);
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
    }

    class ApplyDecisionTask implements Callable<ProcessPartitionKey> {

        private ProcessPartitionKey processPartitionKey;
        private DecisionContainer taskDecision;

        ApplyDecisionTask(ProcessPartitionKey processPartitionKey, DecisionContainer taskDecision) {
            this.processPartitionKey = processPartitionKey;
            this.taskDecision = taskDecision;
        }

        @Override
        public ProcessPartitionKey call() throws Exception {
            logger.trace("Try to apply task decision [{}] for process id [{}]", taskDecision, processPartitionKey.getProcessId());

            // idempotent statement
            DependencyDecision dependencyDecision = dependencyBackend.applyDecision(taskDecision);
            logger.trace("Received dependency decision [{}] after apply task decision [{}]", dependencyDecision, taskDecision);

            if (dependencyDecision.isFail()) {
                logger.warn("Failed apply task decision id [{}]. Apply task decision should be retried after RELEASE_TIMEOUT", taskDecision.getTaskId());

                // RELEASE_TIMEOUT should be automatically fired
                return processPartitionKey;
            }

            List<UUID> readyTasks = dependencyDecision.getReadyTasks();

            if (readyTasks != null) {

                for (UUID taskId2Queue : readyTasks) {
                    // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                    TaskContainer asyncTask = taskBackend.getTask(taskId2Queue);
                    enqueueTask(taskId2Queue, asyncTask.getProcessId(), asyncTask.getActorId(), asyncTask.getStartTime(), getTaskList(asyncTask));
                }

            }

            if (dependencyDecision.isProcessFinished()) {
                processBackend.finishProcess(dependencyDecision.getFinishedProcessId(), dependencyDecision.getFinishedProcessValue());
            }

            taskBackend.addDecisionCommit(taskDecision);

            logger.debug("Successfully apply task decision id [{}] for process id [{}]", taskDecision.getTaskId(), processPartitionKey.getProcessId());

            return processPartitionKey;
        }
    }

    @Override
    public void startProcess(TaskContainer task) {

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
        enqueueTask(task.getTaskId(), task.getProcessId(),task.getActorId(), task.getStartTime(), getTaskList(task));

        processBackend.startProcessCommit(task);

        logger.debug("Successfully start process from task [{}]", task);
    }


    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        if (configBackend.isActorBlocked(ActorUtils.getActorId(actorDefinition))) {
            // TODO: ? We should inform client about block. It should catch exception and try to sleep some time ?
            // TODO: ? Or we should sleep 60 seconds as usual ?
            return null;
        }

        // atomic statement
        UUID taskId = queueBackend.poll(actorDefinition.getFullName(), actorDefinition.getTaskList());

        if (taskId == null) {
            return null;
        }

        // idempotent statement
        final TaskContainer taskContainer = taskBackend.getTaskToExecute(taskId);

        queueBackend.pollCommit(actorDefinition.getFullName(), taskId);

        return taskContainer;
    }


    @Override
    public void release(DecisionContainer taskDecision) {

        // save it firstly
        taskBackend.addDecision(taskDecision);

        UUID taskId = taskDecision.getTaskId();

        // if Error
        if (taskDecision.containsError()) {
            final boolean isShouldBeRestarted = taskDecision.getRestartTime() != TaskDecision.NO_RESTART;

            // enqueue task immediately if needed
            if (isShouldBeRestarted) {

                // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                TaskContainer asyncTask = taskBackend.getTask(taskId);
                logger.debug("Error task enqueued again, taskId [{}]", taskId);
                enqueueTask(taskId, asyncTask.getProcessId(), asyncTask.getActorId(), taskDecision.getRestartTime(), getTaskList(asyncTask));
            }

            taskBackend.addDecisionCommit(taskDecision);

            return;
        }

        synchronized (decisionQueues) {
            ProcessPartitionKey processPartitionKey = new ProcessPartitionKey(taskDecision.getProcessId(), taskServerName);

            if (decisionQueues.containsKey(processPartitionKey)) {
                decisionQueues.get(processPartitionKey).add(taskDecision);
            } else {
                TransferQueue<DecisionContainer> decisionContainers = new LinkedTransferQueue<>();
                decisionContainers.add(taskDecision);
                decisionQueues.put(processPartitionKey, decisionContainers);
            }

            logger.trace("Add task decision [{}] for process queue id [{}]", taskDecision, processPartitionKey);
        }
    }

    private void enqueueTask(UUID taskId, UUID processId, String actorId, long startTime, String taskList) {

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }
        queueBackend.enqueueItem(actorId, taskId, processId, startTime, taskList);
    }


    private String getTaskList(TaskContainer taskContainer) {
        String taskList = null;
        if (taskContainer.getOptions() != null) {
            TaskOptionsContainer taskOptionsContainer = taskContainer.getOptions();
            if (taskOptionsContainer.getActorSchedulingOptions() != null) {
                taskList = taskOptionsContainer.getActorSchedulingOptions().getTaskList();
            }
        }

        return  taskList;
    }
}
