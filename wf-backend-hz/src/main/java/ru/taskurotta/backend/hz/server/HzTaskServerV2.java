package ru.taskurotta.backend.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISet;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
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
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 18.06.13
 * Time: 12:11
 */
public class HzTaskServerV2 extends GeneralTaskServer implements MembershipListener {

    private static final Logger logger = LoggerFactory.getLogger(HzTaskServerV2.class);

    public static final String QUEUE_PREFIX = "#queue#";
    public static final String DECISION_QUEUE_NAMES_SET_NAME = "decisionQueueNamesSet";

    private final int decisionQueuePerNode = 10;
    private boolean isClusterChange = false;

    private HazelcastInstance hazelcastInstance;

    public HzTaskServerV2(BackendBundle backendBundle, HazelcastInstance hazelcastInstance) {
        super(backendBundle);
        this.hazelcastInstance = hazelcastInstance;

        init();
    }

    public HzTaskServerV2(BackendBundle backendBundle) {
        super(backendBundle);
    }

    public HzTaskServerV2(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        super(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend);
    }

    public void init() {
        String memberId = hazelcastInstance.getCluster().getLocalMember().getUuid();

        for (int i = 0; i < decisionQueuePerNode; i++) {
            final String queueName = memberId + QUEUE_PREFIX + i;

            hazelcastInstance.getSet(DECISION_QUEUE_NAMES_SET_NAME).add(queueName);
            logger.debug("Add queue name [{}] to [{}]", queueName, DECISION_QUEUE_NAMES_SET_NAME);

            Executors.newFixedThreadPool(decisionQueuePerNode, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Thread-" + queueName);
                }
            }).submit(new ProcessDecisionJob(queueName));

            logger.debug("Add ProcessDecisionJob for queue [{}]", queueName);
        }
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

        String queueName = determineDecisionQueueName(taskDecision.getProcessId());

        while (queueName == null) {
            logger.warn("Can't determinate decision queue name for process id [{}]", taskDecision.getProcessId());

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                logger.error("Catch exception while sleep", e);
            }

            queueName = determineDecisionQueueName(taskDecision.getProcessId());
        }

        hazelcastInstance.getQueue(queueName).add(taskDecision);
        logger.debug("Add task decision [{}] to queue [{}]", taskDecision, queueName);
    }

    private String determineDecisionQueueName(UUID processId) {
        int membersCount = hazelcastInstance.getCluster().getMembers().size();
        int index = getIndex(processId, membersCount * decisionQueuePerNode);

        ISet<String> set = hazelcastInstance.getSet(DECISION_QUEUE_NAMES_SET_NAME);
        String[] decisionQueueNames = set.toArray(new String[set.size()]);

        if (decisionQueueNames.length <= index) {
            logger.warn("Decision for processId must be placed to [{}] queue, but now only [{}] queues", index, decisionQueueNames.length);
            return null;
        }

        return decisionQueueNames[index];
    }

    private int getIndex(UUID uuid, int maxResult) {
        if (uuid == null || maxResult == 0) {
            return 0;
        }

        return Math.abs(uuid.hashCode() % maxResult);
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        this.isClusterChange = true;
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        this.isClusterChange = true;
    }

    class ProcessDecisionJob implements Callable<Void> {

        boolean running = true;
        private String queueName;

        public ProcessDecisionJob(String queueName) {
            this.queueName = queueName;
            logger.debug("Create thread [{}] for queue name[{}]", Thread.currentThread().getName(), queueName);
        }

        @Override
        public Void call() {
            while (running) {
                try {
                    IQueue<DecisionContainer> queue = hazelcastInstance.getQueue(queueName);

                    if (queue.isEmpty()) {
                        TimeUnit.SECONDS.sleep(1);

                        continue;
                    }

                    DecisionContainer taskDecision = queue.poll();

                    if (isClusterChange) {
                        String reallyDecisionQueueName = determineDecisionQueueName(taskDecision.getProcessId());
                        if (!queueName.equals(reallyDecisionQueueName)) {
                            hazelcastInstance.getQueue(reallyDecisionQueueName).add(taskDecision);

                            continue;
                        }
                    }

                    // idempotent statement
                    DependencyDecision dependencyDecision = dependencyBackend.applyDecision(taskDecision);

                    logger.debug("release() received dependencyDecision = [{}]", dependencyDecision);

                    if (dependencyDecision.isFail()) {

                        logger.debug("release() failed dependencyDecision. release() should be retried after " +
                                "RELEASE_TIMEOUT");

                        // leave release() method.
                        // RELEASE_TIMEOUT should be automatically fired
                        return null;
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
                } catch (Exception e) {
                    logger.error("Error at process task decision", e);
                }
            }

            return null;
        }
    }
}

