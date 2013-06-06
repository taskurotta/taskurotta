package ru.taskurotta.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.PartitionAware;
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
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.model.TaskType;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

    private final IMap<PartitionKey, Queue<DecisionContainer>> decisionQueues;
    private static final String DECISION_QUEUE_MAP_NAME = "decisionQueueMap";

    private int coordinatorPoolSize = 10;

    public HazelcastTaskServer(BackendBundle backendBundle, HazelcastInstance hazelcastInstance) {
        this.processBackend = backendBundle.getProcessBackend();
        this.taskBackend = backendBundle.getTaskBackend();
        this.queueBackend = backendBundle.getQueueBackend();
        this.dependencyBackend = backendBundle.getDependencyBackend();
        this.configBackend = backendBundle.getConfigBackend();

        this.decisionQueues = hazelcastInstance.getMap(DECISION_QUEUE_MAP_NAME);

        new Thread(new Coordinator(coordinatorPoolSize), "Coordinator-" + Thread.currentThread().getName()).start();
    }

    class Coordinator implements Runnable {

        private List<Future<UUID>> futures = new LinkedList<>();

        private ExecutorService executorService;

        Coordinator(int poolSize) {
            this.executorService = Executors.newFixedThreadPool(poolSize);
        }

        @Override
        public void run() {
            logger.trace("Start Coordinator thread [{}]", Thread.currentThread().getName());

            while (true) {

                if (!decisionQueues.isEmpty()) {

                    for(PartitionKey queueId : decisionQueues.keySet()) {

                        if (decisionQueues.isLocked(queueId)) {
                            continue;
                        }

                        if (decisionQueues.get(queueId).isEmpty()) {
                            continue;
                        }

                        decisionQueues.lock(queueId);
                        logger.trace("Lock queue id [{}]", queueId);

                        Future<UUID> future = executorService.submit(new ApplyDecisionTask(decisionQueues.get(queueId).poll()));
                        futures.add(future);
                    }
                }

                if (!futures.isEmpty()) {

                    Iterator<Future<UUID>> iterator = futures.iterator();
                    while (iterator.hasNext()) {
                        Future<UUID> future = iterator.next();

                        if (future.isDone()) {
                            try {
                                UUID queueId = future.get();

                                decisionQueues.unlock(new PartitionKey(queueId, Thread.currentThread().getName()));
                                logger.trace("Unlock queue id [{}]", queueId);

                                futures.remove(future);
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    class ApplyDecisionTask implements Callable<UUID> {

        private DecisionContainer taskDecision;

        ApplyDecisionTask(DecisionContainer taskDecision) {
            this.taskDecision = taskDecision;
        }

        @Override
        public UUID call() throws Exception {
            // idempotent statement
            DependencyDecision dependencyDecision = dependencyBackend.applyDecision(taskDecision);

            logger.debug("release() received dependencyDecision = [{}]", dependencyDecision);

            if (dependencyDecision.isFail()) {

                logger.debug("release() failed dependencyDecision. release() should be retried after " +
                        "RELEASE_TIMEOUT");

                // leave release() method.
                // RELEASE_TIMEOUT should be automatically fired
                return taskDecision.getProcessId();
            }

            List<UUID> readyTasks = dependencyDecision.getReadyTasks();

            if (readyTasks != null) {

                for (UUID taskId2Queue : readyTasks) {

                    // WARNING: This is not optimal code. We are getting whole task only for name and version values.
                    TaskContainer asyncTask = taskBackend.getTask(taskId2Queue);
                    enqueueTask(taskId2Queue, asyncTask.getActorId(), asyncTask.getStartTime(), getTaskList(asyncTask));
                }

            }

            if (dependencyDecision.isProcessFinished()) {
                processBackend.finishProcess(dependencyDecision.getFinishedProcessId(),
                        dependencyDecision.getFinishedProcessValue());
            }

            taskBackend.addDecisionCommit(taskDecision);

            return taskDecision.getProcessId();
        }
    }

    class PartitionKey implements Serializable, PartitionAware {

        private UUID key;
        private Object partitionKey;

        PartitionKey(UUID key, Object partitionKey) {
            this.key = key;
            this.partitionKey = partitionKey;
        }

        UUID getKey() {
            return key;
        }

        void setKey(UUID key) {
            this.key = key;
        }

        void setPartitionKey(Object partitionKey) {
            this.partitionKey = partitionKey;
        }

        @Override
        public Object getPartitionKey() {
            return partitionKey;
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
        enqueueTask(task.getTaskId(), task.getActorId(), task.getStartTime(), getTaskList(task));


        processBackend.startProcessCommit(task);
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
                enqueueTask(taskId, asyncTask.getActorId(), taskDecision.getRestartTime(), getTaskList(asyncTask));
            }

            taskBackend.addDecisionCommit(taskDecision);

            return;
        }

        synchronized (decisionQueues) {
            PartitionKey partitionKey = new PartitionKey(taskDecision.getProcessId(), Thread.currentThread().getName());

            if (decisionQueues.containsKey(partitionKey)) {
                decisionQueues.get(partitionKey).add(taskDecision);
            } else {
                TransferQueue<DecisionContainer> decisionContainers = new LinkedTransferQueue<>();
                decisionContainers.add(taskDecision);
                decisionQueues.put(partitionKey, decisionContainers);
            }
        }
    }

    private void enqueueTask(UUID taskId, String actorId, long startTime, String taskList) {

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }
        queueBackend.enqueueItem(actorId, taskId, startTime, taskList);
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
