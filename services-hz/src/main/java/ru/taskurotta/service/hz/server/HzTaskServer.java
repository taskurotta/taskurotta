package ru.taskurotta.service.hz.server;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Partition;
import com.hazelcast.monitor.LocalExecutorStats;
import com.yammer.metrics.core.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.hz.TaskKey;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.DecisionContainer;

import java.util.UUID;
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
public class HzTaskServer extends GeneralTaskServer {

    private static final Logger logger = LoggerFactory.getLogger(HzTaskServer.class);
    private static final Clock clock = Clock.defaultClock();

    protected ProcessService processService;
    protected HazelcastInstance hzInstance;

    private final String nodeCustomName;
    private final int maxPendingLimit;
    protected final IExecutorService distributedExeService;
    protected final LocalExecutorStats localExecutorStats;

    private final PendingDecisionQueueProxy pendingDecisionQueueProxy;

    protected HzTaskServer(ServiceBundle serviceBundle, HazelcastInstance hzInstance, String nodeCustomName,
                           String decisionProcessingExecutorService, int maxPendingWorkers, int maxPendingLimit,
                           long sleepOnOverloadMls, long timeBeforeDeleteFinishedProcess) {
        super(serviceBundle, timeBeforeDeleteFinishedProcess);

        this.hzInstance = hzInstance;
        this.nodeCustomName = nodeCustomName;
        this.maxPendingLimit = maxPendingLimit;

        processService = serviceBundle.getProcessService();
        distributedExeService = hzInstance.getExecutorService(decisionProcessingExecutorService);
        localExecutorStats = distributedExeService.getLocalExecutorStats();

        pendingDecisionQueueProxy = new PendingDecisionQueueProxy(hzInstance, this, maxPendingWorkers,
                maxPendingLimit, sleepOnOverloadMls);
    }

    protected HzTaskServer(final ProcessService processService, final TaskService taskService,
                           final QueueService queueService,
                           final DependencyService dependencyService, final ConfigService configService,
                           final InterruptedTasksService interruptedTasksService, final GarbageCollectorService garbageCollectorService,
                           HazelcastInstance hzInstance,
                           String nodeCustomName, String decisionProcessingExecutorService, int maxPendingWorkers, int maxPendingLimit,
                           long sleepOnOverloadMls, long timeBeforeDeleteFinishedProcess) {
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
                 public InterruptedTasksService getInterruptedTasksService() {
                     return interruptedTasksService;
                 }

                 @Override
                 public GarbageCollectorService getGarbageCollectorService() {
                     return garbageCollectorService;
                 }

                 @Override
                 public GraphDao getGraphDao() {
                     return null;
                 }

             }, hzInstance, nodeCustomName, decisionProcessingExecutorService, maxPendingWorkers, maxPendingLimit,
                sleepOnOverloadMls, timeBeforeDeleteFinishedProcess);
    }

    public void init() {
    }

    @Override
    public void release(DecisionContainer taskDecision) {

        logger.debug("HZ server release for decision [{}]", taskDecision);
        long startTime = clock.tick();

        // save it firstly
        if (!taskService.finishTask(taskDecision)) {
            logger.warn("{}/{} Task decision can not be saved", taskDecision.getTaskId(), taskDecision.getProcessId());
            return;
        }

        UUID processId = taskDecision.getProcessId();
        TaskKey taskKey = new TaskKey(taskDecision.getTaskId(), processId);

        // is this a partition owner of process id?
        Partition partition = hzInstance.getPartitionService().getPartition(processId);
        if (hzInstance.getCluster().getLocalMember().equals(partition.getOwner())) {
            lockAndProcessDecision(taskKey, this);
        } else {
            // are we overloaded?
            if (localExecutorStats.getPendingTaskCount() > maxPendingLimit) {
                pendingDecisionQueueProxy.stash(taskKey);
            } else {
                sendToClusterMember(taskKey);
            }
        }

        receivedDecisionsCounter.incrementAndGet();
        statRelease.update(clock.tick() - startTime, TimeUnit.NANOSECONDS);
    }

    protected void sendToClusterMember(TaskKey taskKey) {

        ProcessDecisionUnitOfWork call = new ProcessDecisionUnitOfWork(taskKey);
        distributedExeService.submit(call);
    }

    protected DecisionContainer getDecision(UUID taskId, UUID processId) {
        return taskService.getDecision(taskId, processId);
    }


    public static void lockAndProcessDecision(TaskKey taskKey, HzTaskServer taskServer) {

        UUID taskId = taskKey.getTaskId();
        UUID processId = taskKey.getProcessId();

        logger.debug("ProcessDecisionUnitOfWork taskId[{}], processId[{]]", taskId, processId);
        long startTime = clock.tick(), fullTime = clock.tick();

        try {

            taskServer.processService.lock(processId);

            try {
                statPdLock.update(clock.tick() - startTime, TimeUnit.NANOSECONDS);
                startTime = clock.tick();

                taskServer.processDecision(taskId, processId);

                statPdWork.update(clock.tick() - startTime, TimeUnit.NANOSECONDS);
                startTime = clock.tick();
            } finally {
                taskServer.processService.unlock(processId);
            }

            statPdAll.update(clock.tick() - fullTime, TimeUnit.NANOSECONDS);
            processedDecisionsCounter.incrementAndGet();

        } catch (HazelcastInstanceNotActiveException e) {
            // reduce exception rain
            logger.warn(e.getMessage());
        }
    }

}
