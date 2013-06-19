package ru.taskurotta.backend.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.PartitionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
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

    public static final String DECISIONS_MAP_NAME = "decisions";

    private HazelcastInstance hzInstance;

    private static HazelcastTaskServer instance;
    private static final Object instanceMonitor = 0;

    private HazelcastTaskServer(BackendBundle backendBundle) {
        super(backendBundle);
    }

    private HazelcastTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        super(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend);
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

        // save it in cluster memory
        IMap<UUID, DecisionContainer> decisions = hzInstance.getMap(DECISIONS_MAP_NAME);
        decisions.put(taskDecision.getTaskId(), taskDecision);

        // send process call
        ProcessDecisionUnitOfWork call = new ProcessDecisionUnitOfWork(taskDecision.getProcessId(), taskDecision.getTaskId());
        hzInstance.getExecutorService().submit(call);
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
            IMap<UUID, DecisionContainer> decisions = taskServer.getHzInstance().getMap(DECISIONS_MAP_NAME);
            DecisionContainer taskDecision = decisions.get(taskId);
            taskServer.processDecision(taskDecision);
            return null;
        }

        @Override
        public Object getPartitionKey() {
            return processId;
        }
    }
}
