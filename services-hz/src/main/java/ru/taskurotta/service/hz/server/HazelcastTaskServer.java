package ru.taskurotta.service.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.core.PartitionAware;
import com.hazelcast.monitor.LocalExecutorStats;
import com.hazelcast.spring.context.SpringAware;
import com.yammer.metrics.core.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.hz.TaskKey;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.BrokenProcessService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.DecisionContainer;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static ru.taskurotta.util.metrics.HzTaskServerMetrics.statPdAll;
import static ru.taskurotta.util.metrics.HzTaskServerMetrics.statPdLock;
import static ru.taskurotta.util.metrics.HzTaskServerMetrics.statPdWork;
import static ru.taskurotta.util.metrics.HzTaskServerMetrics.statRelease;

/**
 * Task server with async decision processing.
 * Behaves exactly like GeneralTaskServer except for overridden release() method
 * Created by void 18.06.13 18:39
 */
public class HazelcastTaskServer extends GeneralTaskServer {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastTaskServer.class);
    private static final Clock clock = Clock.defaultClock();

    private static final String LOCK_PROCESS_MAP_NAME = HazelcastTaskServer.class.getName() + "#lockProcessMap";

    protected HazelcastInstance hzInstance;
    private IMap<UUID, ?> lockProcessMap;

    private final String nodeCustomName;
    private final int maxPendingLimit;
    protected final IExecutorService distributedExeService;
    protected final LocalExecutorStats localExecutorStats;

    private final PendingDecisionQueueProxy pendingDecisionQueueProxy;

    protected HazelcastTaskServer(ServiceBundle serviceBundle, HazelcastInstance hzInstance, String nodeCustomName,
                                  String decisionProcessingExecutorService, int maxPendingWorkers, int maxPendingLimit,
                                  long sleepOnOverloadMls) {
        super(serviceBundle);

        this.hzInstance = hzInstance;
        this.nodeCustomName = nodeCustomName;
        this.maxPendingLimit = maxPendingLimit;

        lockProcessMap = hzInstance.getMap(LOCK_PROCESS_MAP_NAME);
        distributedExeService = hzInstance.getExecutorService(decisionProcessingExecutorService);
        localExecutorStats = distributedExeService.getLocalExecutorStats();

        pendingDecisionQueueProxy = new PendingDecisionQueueProxy(hzInstance, this, maxPendingWorkers,
                maxPendingLimit, sleepOnOverloadMls);
    }

    protected HazelcastTaskServer(final ProcessService processService, final TaskService taskService,
                                  final QueueService queueService,
                                  final DependencyService dependencyService, final ConfigService configService,
                                  final BrokenProcessService brokenProcessService, final GarbageCollectorService garbageCollectorService,
                                  HazelcastInstance hzInstance,
                                  String nodeCustomName, String decisionProcessingExecutorService, int maxPendingWorkers, int maxPendingLimit,
                                  long sleepOnOverloadMls) {
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
        }, hzInstance, nodeCustomName, decisionProcessingExecutorService, maxPendingWorkers, maxPendingLimit,
        sleepOnOverloadMls);
    }

    public void init() {
    }

    @Override
    public void release(DecisionContainer taskDecision) {

        logger.debug("HZ server release for decision [{}]", taskDecision);
        long startTime = clock.tick();

        // save it firstly
        taskService.addDecision(taskDecision);

        TaskKey taskKey = new TaskKey(taskDecision.getTaskId(), taskDecision.getProcessId());

        if (localExecutorStats.getPendingTaskCount() > maxPendingLimit) {
            pendingDecisionQueueProxy.stash(taskKey);
        } else {
            sendToClusterMember(taskKey);
        }

        startedDistributedTasks.incrementAndGet();
        statRelease.update(clock.tick() - startTime, TimeUnit.NANOSECONDS);
    }

    protected void sendToClusterMember(TaskKey taskKey) {
        ProcessDecisionUnitOfWork call = new ProcessDecisionUnitOfWork(taskKey);
        distributedExeService.submit(call);
    }

    protected DecisionContainer getDecision(UUID taskId, UUID processId) {
        return taskService.getDecision(taskId, processId);
    }


    public static void lockAndProcessDecision(TaskKey taskKey, HazelcastTaskServer taskServer) {

        UUID taskId = taskKey.getTaskId();
        UUID processId = taskKey.getProcessId();

        logger.debug("ProcessDecisionUnitOfWork taskId[{}], processId[{]]", taskId, processId);
        long startTime = clock.tick(), fullTime = clock.tick();

        try {

            taskServer.lockProcessMap.lock(processId);

            statPdLock.update(clock.tick() - startTime, TimeUnit.NANOSECONDS);
            startTime = clock.tick();

            try {
                taskServer.processDecision(taskId, processId);

                statPdWork.update(clock.tick() - startTime, TimeUnit.NANOSECONDS);
                startTime = clock.tick();
            } finally {
                taskServer.lockProcessMap.unlock(processId);
            }

            statPdAll.update(clock.tick() - fullTime, TimeUnit.NANOSECONDS);
            finishedDistributedTasks.incrementAndGet();

        } catch (HazelcastInstanceNotActiveException e) {
            // reduce exception rain
            logger.warn(e.getMessage());
        }
    }
    /**
     * Callable task for processing taskDecisions
     */
    @SpringAware
    public static class ProcessDecisionUnitOfWork implements Callable, PartitionAware, Serializable {
        private static final Logger logger = LoggerFactory.getLogger(ProcessDecisionUnitOfWork.class);

        TaskKey taskKey;
        HazelcastTaskServer taskServer;

        public ProcessDecisionUnitOfWork() {
        }

        public ProcessDecisionUnitOfWork(TaskKey TaskKey) {
            this.taskKey = TaskKey;
        }

        @Autowired
        public void setTaskServer(HazelcastTaskServer taskServer) {
            this.taskServer = taskServer;
        }

        @Override
        public Object call() throws Exception {
            try {
                lockAndProcessDecision(taskKey, taskServer);
            } catch (RuntimeException ex) {
                logger.error("Can not process task decision", ex);
                throw ex;
            }

            return null;
        }

        @Override
        public Object getPartitionKey() {
            return taskKey.getProcessId();
        }

        public TaskKey getTaskKey() {
            return taskKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ProcessDecisionUnitOfWork that = (ProcessDecisionUnitOfWork) o;

            if (taskKey != null ? !taskKey.equals(that.taskKey) : that.taskKey != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return taskKey != null ? taskKey.hashCode() : 0;
        }
    }

}
