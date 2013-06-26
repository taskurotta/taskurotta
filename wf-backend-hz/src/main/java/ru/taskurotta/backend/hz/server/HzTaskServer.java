package ru.taskurotta.backend.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IQueue;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Task server with async decision processing.
 * Behaves exactly like GeneralTaskServer except for overridden release() method
 * User: dimadin
 * Date: 10.06.13 17:53
 */
public class HzTaskServer extends GeneralTaskServer implements MembershipListener {

    private static final String QUEUE_PREFIX = "#queue#";

    private final static Logger logger = LoggerFactory.getLogger(HzTaskServer.class);

    private HazelcastInstance hzInstance;

    private int queuesPerNode = 5;

    private String decisionQueueNamesList = "decisionQueueNames";

    public HzTaskServer(BackendBundle backendBundle) {
        super(backendBundle);
    }

    public HzTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        super(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend);
    }

    @Override
    public void release(DecisionContainer taskDecision) {
        String decisionQueueName = determineDecisionQueueName(taskDecision.getProcessId());

        if(logger.isDebugEnabled()) {
            IList<String[]> debug = hzInstance.getList("debugList");
            debug.add(new String[]{taskDecision.getProcessId().toString(), decisionQueueName});
        }

        Queue<DecisionContainer> queue = hzInstance.getQueue(decisionQueueName);
        logger.debug("Queue[{}] was used for releasing DecisionContainer[{}]", decisionQueueName, taskDecision);
        queue.add(taskDecision);
    }


    //Determine the queue for storing taskDecisions with the given processId. Same processIds go to the same queue
    private String determineDecisionQueueName(UUID processID) {
        int membersCount = hzInstance.getCluster().getMembers().size();
        Integer nodeIndex = getIndex(processID, membersCount);
        logger.trace("determineDecisionQueueName for [{}]: membersCount[{}], nodeIndex[{}]", processID, membersCount, nodeIndex);

        Set<String> decisionQueues = hzInstance.getSet(decisionQueueNamesList);
        if(decisionQueues == null || decisionQueues.isEmpty()) {
            throw new IllegalStateException("Cannot determine queue for decision release: no queues found");
        }
        String result = decisionQueues.iterator().next();//default value to prevent NPE
        Map<String, Set<String>> nodesQueues = getQueuesGroupedByNode(decisionQueues);
        logger.trace("Nodes queues getted are [{}]", nodesQueues);

        int memberIdx = 0;
        for(String memberId: nodesQueues.keySet()) {
            if(memberIdx == nodeIndex) {
                Set<String> nodeQueueNames = nodesQueues.get(memberId);
                Integer queueIndexOnNode = getIndex(processID, nodeQueueNames.size());
                for(String queueName: nodeQueueNames) {
                    if(queueName.endsWith(String.valueOf(queueIndexOnNode))) {
                        result = queueName;
                        logger.trace("Queue name determined for decision for process[{}] is[{}]", processID, queueName);
                        break;
                    }
                }
                break;
            }
            memberIdx++;
        }

        return result;
    }

    //Groups queues names by node id's  Map<nodeId, Set<NodeQueueNames>>
    private Map<String, Set<String>> getQueuesGroupedByNode(Set<String> names) {
        Map<String, Set<String>> result = new HashMap<>();
        if(names!=null && !names.isEmpty()) {
            for(String name: names) {
                String memberId = name.split(QUEUE_PREFIX)[0];
                if(!result.containsKey(memberId)) {
                    result.put(memberId, new HashSet<String>());
                }
                result.get(memberId).add(name);
            }
        }
        return result;
    }

    //Creates this node's queues in HZ map store and starts single thread per local queue for processing
    public void init() {
        hzInstance.getCluster().addMembershipListener(this);
        String memberId = hzInstance.getCluster().getLocalMember().getUuid();
        for(int i = 0; i<queuesPerNode; i++) {
            String queueName = memberId + QUEUE_PREFIX + i;
            hzInstance.getSet(decisionQueueNamesList).add(queueName);
            Thread thread = new Thread(new ProcessDecisionJob(queueName));
            thread.setDaemon(true);
            thread.start();
        }
        logger.debug("Started [{}] thread-for-queue's for taskDecision processing", queuesPerNode);

        if(logger.isDebugEnabled()) { //Debug monitor for logging decision queues contents
            Thread monitor = new Thread() {

                @Override
                public void run() {
                    while(true) {
                        try {
                            Thread.sleep(20000);

                            StringBuilder sb = new StringBuilder();
                            Set<String> queueNames = hzInstance.getSet(decisionQueueNamesList);
                            for(String queueName: queueNames) {
                                Queue<DecisionContainer> queue = hzInstance.getQueue(queueName);
                                DecisionContainer[] content = queue.toArray(new DecisionContainer[queue.size()]);
                                sb.append("\n").append("Queue: ").append(queueName).append(", size: ").append(queue.size()).append(", content: [").append(Arrays.asList(content)).append("]");
                            }
                            logger.debug("Decision queue monitor: " + sb.toString());

                            StringBuilder sb2 = new StringBuilder();
                            Map<String, Set<String>> spreadingMap = getAsMap(hzInstance.<String[]>getList("debugList"));
                            for(String str: spreadingMap.keySet()) {
                                sb2.append("\n").append("ProcessId [").append(str).append("]: ").append(spreadingMap.get(str));
                            }

                            logger.debug("Release spreading monitor: " + sb2.toString());

                        } catch(Throwable e) {
                            logger.warn("Debug monitor failed", e);
                        }
                    }
                }

            };
            monitor.setDaemon(true);
            monitor.start();
        }

    }

    private Map<String, Set<String>> getAsMap(IList<String[]> debugList) {
        Map<String, Set<String>> result = new HashMap<>();
        for(String[] val:  debugList) {
            Set<String> set = null;
            if(result.containsKey(val[0])) {
                set = result.get(val[0]);
            } else {
                set = new HashSet<>();
            }
             set.add(val[1]);
            result.put(val[0], set);
        }
        return result;
    }

    //Job to process task release. Should always execute in single thread
    protected class ProcessDecisionJob implements Runnable {

        boolean running = true;
        private String queueName;

        public ProcessDecisionJob(String queueName) {
            this.queueName = queueName;
            logger.debug("Thread for queueName[{}] created", queueName);
        }

        @Override
        public void run() {

            while(running) {

                if(Thread.currentThread().isInterrupted()){
                    logger.warn("Thread[{}] interruption detected, breaking the cycle", Thread.currentThread().getName());
                    break;
                }

                try {
                    Queue<DecisionContainer> queue = hzInstance.getQueue(queueName);

                    DecisionContainer taskDecision = queue.poll();

                    if(taskDecision == null) {
                        Thread.sleep(200);
                        continue;
                    }

                    logger.debug("Processing queue[{}] with decision[{}]", queueName, taskDecision);

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


                    // idempotent statement
                    DependencyDecision dependencyDecision = dependencyBackend.applyDecision(taskDecision);

                    logger.debug("release() received dependencyDecision = [{}]", dependencyDecision);

                    if (dependencyDecision.isFail()) {

                        logger.debug("release() failed dependencyDecision. release() should be retried after " +
                                "RELEASE_TIMEOUT");

                        // leave release() method.
                        // RELEASE_TIMEOUT should be automatically fired
                        return;
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
                        processBackend.finishProcess(dependencyDecision,
                                dependencyDecision.getFinishedProcessValue());
                    }

                    taskBackend.addDecisionCommit(taskDecision);

                } catch(Throwable e) {
                    logger.error("Error at process task decision", e);
                }
            }
        }
    }

    //Same uuid mapped to same int result
    private int getIndex(UUID uuid, int maxResult) {
        int result = 0;
        if(uuid!=null && maxResult>0) {
            result = Math.abs(uuid.hashCode()%maxResult);
        }
        return result;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    public void setQueuesPerNode(int queuesPerNode) {
        this.queuesPerNode = queuesPerNode;
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        logger.debug("MemberId[{}] added to cluster", membershipEvent.getMember().getUuid());
    }

    @Override
    //Queues contents processed by removed member should be redistributed and queues removed
    public void memberRemoved(MembershipEvent membershipEvent) {
        String memberId = membershipEvent.getMember().getUuid();
        logger.debug("MemberId[{}] removed from cluster. Try to redistribute its queues content", memberId);

        Set<String> decisionQueues = hzInstance.getSet(decisionQueueNamesList);
        ILock lock = hzInstance.getLock(decisionQueues);
        try {
            lock.lock();
            Queue<DecisionContainer> targetQueue = getQueueToDrainTo(decisionQueues);
            Set <String> queueNamesToDump = getQueuesGroupedByNode(decisionQueues).get(memberId);
            logger.debug("Queues to dump are [{}]", queueNamesToDump);
            if(queueNamesToDump!=null && !queueNamesToDump.isEmpty()) {
                for(String name: queueNamesToDump)  {
                    IQueue<DecisionContainer> queue = hzInstance.getQueue(name);
                    if(queue!=null) {
                        DecisionContainer[] items = queue.toArray(new DecisionContainer[queue.size()]);
                        if(items!=null) {
                            targetQueue.addAll(Arrays.asList(items));
                            logger.warn("[{}] items drained from [{}] on member[{}] removal", items.length, name, memberId);
                            queue.destroy();
                            decisionQueues.remove(name);
                            logger.warn("Queue [{}] removed", name);
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }

    }


    //Returns queue of this node for drain tasks to
    private Queue<DecisionContainer> getQueueToDrainTo(Set<String> decisionQueueNames) {
        String memberId = hzInstance.getCluster().getLocalMember().getUuid();

        Set<String> localNodeQueues = getQueuesGroupedByNode(decisionQueueNames).get(memberId);

        String targetQueueName = localNodeQueues.iterator().next();

        logger.debug("Target queue name getted for memberId[{}] is [{}]", memberId, targetQueueName);
        return hzInstance.getQueue(targetQueueName);
    }

    public void setDecisionQueueNamesList(String decisionQueueNamesList) {
        this.decisionQueueNamesList = decisionQueueNamesList;
    }
}
