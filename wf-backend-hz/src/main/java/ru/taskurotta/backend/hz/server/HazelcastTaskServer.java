package ru.taskurotta.backend.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.PartitionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.statistics.Metrics;
import ru.taskurotta.backend.statistics.StaticMetrics;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.transport.model.DecisionContainer;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task server with async decision processing.
 * Behaves exactly like GeneralTaskServer except for overridden release() method
 * Created by void 18.06.13 18:39
 */
public class HazelcastTaskServer extends GeneralTaskServer {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastTaskServer.class);

    private static final Metrics.CheckPoint chpRelease = StaticMetrics.create("HazelcastTaskServer.release");
    private static final Metrics.CheckPoint chpProcessDecision_async = StaticMetrics.create("HazelcastTaskServer.processDecision_async");

    protected HazelcastInstance hzInstance;

    protected String executorServiceName = "decisionProcessingExecutorService";

    protected static HazelcastTaskServer instance;
    protected static final Object instanceMonitor = 0;

    private String nodeCustomName = "undefined";

    protected HazelcastTaskServer(BackendBundle backendBundle) {
        super(backendBundle);
    }

    protected HazelcastTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
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

    /*
    FOR TESTS ONLY
    * */
    public static void setInstance(HazelcastTaskServer instance) {
        HazelcastTaskServer.instance = instance;
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

        logger.debug("HZ server release for decision [{}]", taskDecision);

        long smt = System.currentTimeMillis();

        // save it in task backend
        taskBackend.addDecision(taskDecision);

        // send process call
        ProcessDecisionUnitOfWork call = new ProcessDecisionUnitOfWork(taskDecision.getProcessId(), taskDecision.getTaskId());
        hzInstance.getExecutorService(executorServiceName).submit(call);

        chpRelease.mark(smt);
    }

    protected DecisionContainer getDecision(UUID taskId, UUID processId) {
        return taskBackend.getDecision(taskId, processId);
    }

    /**
     * Callable task for processing taskDecisions
     */
    public static class ProcessDecisionUnitOfWork implements Callable, PartitionAware, Serializable {
        private static AtomicInteger counter = new AtomicInteger(0);

        private static final Logger logger = LoggerFactory.getLogger(ProcessDecisionUnitOfWork.class);
        UUID processId;
        UUID taskId;
        String jobId = "undefined";

        public ProcessDecisionUnitOfWork() {
        }

        public ProcessDecisionUnitOfWork(UUID processId, UUID taskId) {
            this.processId = processId;
            this.taskId = taskId;

        }

        @Override
        public Object call() throws Exception {

            long mst = System.currentTimeMillis();

            HazelcastTaskServer taskServer = HazelcastTaskServer.getInstance();
            HazelcastInstance taskHzInstance = taskServer.getHzInstance();

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

                chpProcessDecision_async.mark(mst);
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
