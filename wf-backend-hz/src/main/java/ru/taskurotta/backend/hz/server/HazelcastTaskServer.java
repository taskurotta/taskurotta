package ru.taskurotta.backend.hz.server;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.PartitionAware;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.hz.Constants;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.transport.model.DecisionContainer;

/**
 * Task server with async decision processing.
 * Behaves exactly like GeneralTaskServer except for overridden release() method
 * Created by void 18.06.13 18:39
 */
public class HazelcastTaskServer extends GeneralTaskServer {

    private HazelcastInstance hzInstance;

    private String executorServiceName = Constants.DEFAULT_EXECUTOR_SERVICE_NAME;

    private static HazelcastTaskServer instance;
    private static final Object instanceMonitor = 0;

    private HazelcastTaskServer(BackendBundle backendBundle) {
        super(backendBundle);
    }

    private HazelcastTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        super(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend);
    }

    public static HazelcastTaskServer createInstance(BackendBundle backendBundle) {
        synchronized (instanceMonitor) {
            if (null == instance) {
                instance = new HazelcastTaskServer(backendBundle);
                instanceMonitor.notifyAll();
            }
        }
        return instance;
    }

    public static HazelcastTaskServer createInstance(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        synchronized (instanceMonitor) {
            if (null == instance) {
                instance = new HazelcastTaskServer(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend);
                instanceMonitor.notifyAll();
            }
        }
        return instance;
    }

    //For obtaining reference to current TaskServer instance when processing async decision
    public static HazelcastTaskServer getInstance() throws InterruptedException {
        synchronized (instanceMonitor) {
            if (null == instance) {
                instanceMonitor.wait();
            }
        }
        return instance;
    }

    public HazelcastInstance getHzInstance() {
        return hzInstance;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    public void init() {
    }

    @Override
    public void release(DecisionContainer taskDecision) {
        logger.debug("releasing DecisionContainer[{}]", taskDecision);

        // save it in task backend
        taskBackend.addDecision(taskDecision);

        // send process call
        ProcessDecisionUnitOfWork call = new ProcessDecisionUnitOfWork(taskDecision.getProcessId(), taskDecision.getTaskId());
        hzInstance.getExecutorService(executorServiceName).submit(call);
    }

    /**
     * Callable task for processing taskDecisions
     */
    public static class ProcessDecisionUnitOfWork implements Callable, PartitionAware, Serializable {
        UUID processId;
        UUID taskId;

        public ProcessDecisionUnitOfWork() {
        }

        public ProcessDecisionUnitOfWork(UUID processId, UUID taskId) {
            this.processId = processId;
            this.taskId = taskId;
        }

        @Override
        public Object call() throws Exception {
            HazelcastTaskServer taskServer = HazelcastTaskServer.getInstance();
            HazelcastInstance taskHzInstance = taskServer.getHzInstance();

            ILock lock = taskHzInstance.getLock(processId);
            try {
                lock.lock();
                DecisionContainer taskDecision = taskServer.taskBackend.getDecision(taskId, processId);
                if (taskServer.processDecision(taskDecision)) {
                    //TODO: snapshot processsing here?
                }
//                logMemberAction(taskHzInstance);
            } finally {
//                validate(taskHzInstance);
                lock.unlock();
            }
            return null;
        }

//        private void logMemberAction(HazelcastInstance hzInstance) {
//            String memberName = hzInstance.getCluster().getLocalMember().toString();
//            hzInstance.getSet(memberName+":pocTestSet").add(processId);
//        }

//        private void validate(HazelcastInstance hzInstance) {
//            Set<UUID> result = new HashSet();
//            int validated = 0;
//            for(Member member: hzInstance.getCluster().getMembers()) {
//                Set<UUID> memberSet = hzInstance.getSet(member.toString()+":pocTestSet");
//                for(UUID uuid: memberSet) {
//                    boolean newVal = result.add(uuid);
//                    if(!newVal) {
//                        logger.error("Same process[{}] on different nodes detected!", uuid);
//                        return;
//                    } else {
//                        validated++;
//                    }
//                }
//            }
//            logger.info("Validated [{}] processes, all OK", validated);
//        }

        @Override
        public Object getPartitionKey() {
            return processId;
        }
    }

    public void setExecutorServiceName(String executorServiceName) {
        this.executorServiceName = executorServiceName;
    }
}
