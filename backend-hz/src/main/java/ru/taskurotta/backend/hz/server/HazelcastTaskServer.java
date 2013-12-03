package ru.taskurotta.backend.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IMap;
import com.hazelcast.core.PartitionAware;
import com.hazelcast.spring.context.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.gc.AbstractGCBackend;
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
    private static final String LOCK_PROCESS_MAP_NAME = HazelcastTaskServer.class.getName() + "#lockProcessMap";

    protected HazelcastInstance hzInstance;
    protected IMap lockProcessMap;

    protected String decisionProcessingExecutorService;

    protected static HazelcastTaskServer instance;
    protected static final Object instanceMonitor = 0;

    private String nodeCustomName;

    protected HazelcastTaskServer(BackendBundle backendBundle, HazelcastInstance hzInstance, String nodeCustomName,
                                  String decisionProcessingExecutorService) {
        super(backendBundle);

        this.hzInstance = hzInstance;
        this.nodeCustomName = nodeCustomName;
        this.decisionProcessingExecutorService = decisionProcessingExecutorService;

        lockProcessMap = hzInstance.getMap(LOCK_PROCESS_MAP_NAME);
    }

    protected HazelcastTaskServer(final ProcessBackend processBackend, final TaskBackend taskBackend,
                                  final QueueBackend queueBackend,
                                  final DependencyBackend dependencyBackend, final ConfigBackend configBackend,
                                  final BrokenProcessBackend brokenProcessBackend, HazelcastInstance hzInstance,
                                  String nodeCustomName, String decisionProcessingExecutorService) {
        this(new BackendBundle() {
            @Override
            public ProcessBackend getProcessBackend() {
                return processBackend;
            }

            @Override
            public TaskBackend getTaskBackend() {
                return taskBackend;
            }

            @Override
            public QueueBackend getQueueBackend() {
                return queueBackend;
            }

            @Override
            public DependencyBackend getDependencyBackend() {
                return dependencyBackend;
            }

            @Override
            public ConfigBackend getConfigBackend() {
                return configBackend;
            }

            @Override
            public BrokenProcessBackend getBrokenProcessBackend() {
                return brokenProcessBackend;
            }

            @Override
            public AbstractGCBackend getGCBackend() {
                return null;
            }
        },  hzInstance, nodeCustomName, decisionProcessingExecutorService);
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
        hzInstance.getExecutorService(decisionProcessingExecutorService).submit(call);
    }

    protected DecisionContainer getDecision(UUID taskId, UUID processId) {
        return taskBackend.getDecision(taskId, processId);
    }

    /**
     * Callable task for processing taskDecisions
     */
    @SpringAware
    public static class ProcessDecisionUnitOfWork implements Callable, PartitionAware, Serializable {
        private static final Logger logger = LoggerFactory.getLogger(ProcessDecisionUnitOfWork.class);

        UUID processId;
        UUID taskId;
        HazelcastTaskServer taskServer;

        public ProcessDecisionUnitOfWork() {
        }

        public ProcessDecisionUnitOfWork(UUID processId, UUID taskId) {
            this.processId = processId;
            this.taskId = taskId;

        }

        @Autowired
        public void setTaskServer(HazelcastTaskServer taskServer) {
            this.taskServer = taskServer;
        }

        @Override
        public Object call() throws Exception {

            try {

                taskServer.lockProcessMap.lock(processId);

                try {
                    DecisionContainer taskDecision = taskServer.getDecision(taskId, processId);
                    if (taskDecision == null) {
                        String error = "Cannot get task decision from store by taskId[" + taskId + "], processId[" + processId + "]";
                        logger.error(error);
                        //TODO: this exception disappears for some reason
                        throw new IllegalStateException(error);
                    }

                    taskServer.processDecision(taskDecision);

                } finally {
                    taskServer.lockProcessMap.unlock(processId);
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

}
