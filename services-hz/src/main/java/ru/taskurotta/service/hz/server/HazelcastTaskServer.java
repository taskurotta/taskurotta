package ru.taskurotta.service.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IMap;
import com.hazelcast.core.PartitionAware;
import com.hazelcast.spring.context.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.storage.BrokenProcessService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
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

    protected HazelcastTaskServer(ServiceBundle serviceBundle, HazelcastInstance hzInstance, String nodeCustomName,
                                  String decisionProcessingExecutorService) {
        super(serviceBundle);

        this.hzInstance = hzInstance;
        this.nodeCustomName = nodeCustomName;
        this.decisionProcessingExecutorService = decisionProcessingExecutorService;

        lockProcessMap = hzInstance.getMap(LOCK_PROCESS_MAP_NAME);
    }

    protected HazelcastTaskServer(final ProcessService processService, final TaskService taskService,
                                  final QueueService queueService,
                                  final DependencyService dependencyService, final ConfigService configService,
                                  final BrokenProcessService brokenProcessService, final GarbageCollectorService garbageCollectorService,
                                  HazelcastInstance hzInstance,
                                  String nodeCustomName, String decisionProcessingExecutorService) {
        this(new ServiceBundle() {
            @Override
            public ProcessService getProcessService() {
                return processService;
            }

            @Override
            public TaskService getTaskService() {
                return taskService;
            }

            @Override
            public QueueService getQueueService() {
                return queueService;
            }

            @Override
            public DependencyService getDependencyService() {
                return dependencyService;
            }

            @Override
            public ConfigService getConfigService() {
                return configService;
            }

            @Override
            public BrokenProcessService getBrokenProcessService() {
                return brokenProcessService;
            }

            @Override
            public GarbageCollectorService getGarbageCollectorService() {
                return garbageCollectorService;
            }
        },  hzInstance, nodeCustomName, decisionProcessingExecutorService);
    }

    public void init() {
    }

    @Override
    public void release(DecisionContainer taskDecision) {

        logger.debug("HZ server release for decision [{}]", taskDecision);

        // send process call
        ProcessDecisionUnitOfWork call = new ProcessDecisionUnitOfWork(taskDecision);
        hzInstance.getExecutorService(decisionProcessingExecutorService).submit(call);
    }

    protected DecisionContainer getDecision(UUID taskId, UUID processId) {
        return taskService.getDecision(taskId, processId);
    }

    /**
     * Callable task for processing taskDecisions
     */
    @SpringAware
    public static class ProcessDecisionUnitOfWork implements Callable, PartitionAware, Serializable {
        private static final Logger logger = LoggerFactory.getLogger(ProcessDecisionUnitOfWork.class);

        DecisionContainer taskDecision;
        HazelcastTaskServer taskServer;

        public ProcessDecisionUnitOfWork() {
        }

        public ProcessDecisionUnitOfWork(DecisionContainer taskDecision) {
            this.taskDecision = taskDecision;
        }

        @Autowired
        public void setTaskServer(HazelcastTaskServer taskServer) {
            this.taskServer = taskServer;
        }

        @Override
        public Object call() throws Exception {

            logger.debug("ProcessDecisionUnitOfWork decision is[{}], taskId[{}], processId[{]]", taskDecision,
                    taskDecision.getTaskId(), taskDecision.getProcessId());

            try {

                UUID processId = taskDecision.getProcessId();

                taskServer.lockProcessMap.lock(processId);

                try {

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
            return taskDecision.getProcessId();
        }
    }

}
