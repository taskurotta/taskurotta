package ru.taskurotta.backend.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.ILock;
import com.hazelcast.core.PartitionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.transport.model.DecisionContainer;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Task server with async decision processing.
 * Behaves exactly like GeneralTaskServer except for overridden release() method
 * Created by void 18.06.13 18:39
 */
public class HazelcastTaskServer extends GeneralTaskServer {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastTaskServer.class);

    protected HazelcastInstance hzInstance;

    protected String executorServiceName = "decisionProcessingExecutorService";

    protected static HazelcastTaskServer instance;
    protected static final Object instanceMonitor = 0;

    private String nodeCustomName = "undefined";

    protected HazelcastTaskServer(BackendBundle backendBundle) {
        super(backendBundle);
    }

    protected HazelcastTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend,
                                  DependencyBackend dependencyBackend, ConfigBackend configBackend, BrokenProcessBackend brokenProcessBackend) {
        super(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend, brokenProcessBackend);
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

    /*
    FOR TESTS ONLY
    * */
    public static void setInstance(HazelcastTaskServer instance) {
        HazelcastTaskServer.instance = instance;
    }

    public static HazelcastTaskServer createInstance(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend, BrokenProcessBackend brokenProcessBackend) {
        synchronized (instanceMonitor) {
            if (null == instance) {
                instance = new HazelcastTaskServer(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend, brokenProcessBackend);
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

        logger.debug("HZ server release for decision [{}]", taskDecision);

        // save it in task backend
        taskBackend.addDecision(taskDecision);

        // send process call
        ProcessDecisionUnitOfWork call = new ProcessDecisionUnitOfWork(taskDecision.getProcessId(), taskDecision.getTaskId());
        hzInstance.getExecutorService(executorServiceName).submit(call);
    }

    protected DecisionContainer getDecision(UUID taskId, UUID processId) {
        return taskBackend.getDecision(taskId, processId);
    }

    /**
     * Callable task for processing taskDecisions
     */
    public static class ProcessDecisionUnitOfWork implements Callable, PartitionAware, Serializable {
        private static final Logger logger = LoggerFactory.getLogger(ProcessDecisionUnitOfWork.class);

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

            try {

                ILock lock = taskHzInstance.getLock(processId);

                try {
                    lock.lock();
                    DecisionContainer taskDecision = taskServer.getDecision(taskId, processId);
                    if (taskDecision == null) {
                        String error = "Cannot get task decision from store by taskId[" + taskId + "], processId[" + processId + "]";
                        logger.error(error);
                        //TODO: this exception disappears for some reason
                        throw new IllegalStateException(error);
                    }

                    taskServer.processDecision(taskDecision);

                } finally {
                    lock.unlock();
                }

            } catch (HazelcastInstanceNotActiveException e) {
                // reduce exception rain
                logger.warn(e.getMessage());
            }

            return null;
        }

        @Override
        public Object getPartitionKey() {
            return processId;
        }
    }

    public void setExecutorServiceName(String executorServiceName) {
        this.executorServiceName = executorServiceName;
    }

    public String getNodeCustomName() {
        return nodeCustomName;
    }

    public void setNodeCustomName(String nodeCustomName) {
        this.nodeCustomName = nodeCustomName;
    }
}
