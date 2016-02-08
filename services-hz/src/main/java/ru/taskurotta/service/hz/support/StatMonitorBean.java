package ru.taskurotta.service.hz.support;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.monitor.LocalExecutorStats;
import com.hazelcast.monitor.LocalMapStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.LocalCachedQueueStats;
import ru.taskurotta.hazelcast.queue.delay.MongoStorageFactory;
import ru.taskurotta.hazelcast.queue.impl.QueueContainer;
import ru.taskurotta.hazelcast.queue.store.mongodb.MongoCachedQueueStore;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.service.console.retriever.StatInfoRetriever;
import ru.taskurotta.service.hz.gc.HzGarbageCollectorService;
import ru.taskurotta.service.hz.gc.LostGraphCleaner;
import ru.taskurotta.service.hz.gc.LostProcessCleaner;
import ru.taskurotta.service.hz.queue.HzQueueService;
import ru.taskurotta.service.recovery.impl.RecoveryServiceImpl;
import ru.taskurotta.service.recovery.impl.RecoveryThreadsImpl;
import ru.taskurotta.util.DaemonThread;
import ru.taskurotta.util.metrics.HzTaskServerMetrics;

import java.io.Serializable;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created on 14.05.2015.
 */
public class StatMonitorBean implements StatInfoRetriever {

    private static final Logger logger = LoggerFactory.getLogger(StatMonitorBean.class);

    private HazelcastInstance hazelcastInstance;
    private IExecutorService executorService;

    private int periodSec;

    public static class FinishedProcessesCounterCallable implements Callable<Integer>, Serializable {

        @Override
        public Integer call() throws Exception {
            return GeneralTaskServer.finishedProcessesCounter.get();
        }
    }

    public StatMonitorBean(HazelcastInstance hazelcastInstance, int periodSec) {
        this.hazelcastInstance = hazelcastInstance;
        this.periodSec = periodSec;

        executorService = hazelcastInstance.getExecutorService(getClass().getName());
    }

    public void init() {
        if (periodSec > 0) {
            logger.info("=========== Periodic stats monitor activated =============");

            // start dump thread
            new DaemonThread("stats dumper", TimeUnit.SECONDS, periodSec) {

                @Override
                public void daemonJob() {
                    logger.info(getNodeStats());
                    logger.info(getHazelcastStats());
                }

            }.start();

        }

    }

    public static String bytesToMb(long bytes) {
        return new Formatter().format("%6.2f", ((double) bytes / 1024 / 1024)).toString();
    }


    @Override
    public String getHazelcastStats() {


        StringBuilder sb = new StringBuilder();

        if (hazelcastInstance != null) {
            long totalHeapCost = 0;

            sb.append("\n============  ").append(hazelcastInstance.getName()).append("  ===========");

            for (DistributedObject distributedObject : hazelcastInstance.getDistributedObjects()) {
                sb.append(String.format("\n%22s -> %18s", distributedObject.getServiceName(),
                        distributedObject.getName()));

                if (distributedObject instanceof IMap) {
                    IMap map = (IMap) distributedObject;
                    LocalMapStats statM = map.getLocalMapStats();

                    sb.append("\tsize = " + map.size());
                    sb.append("\tcache: size = " + statM.getOwnedEntryCount());
                    sb.append("\tmem = " + bytesToMb(statM.getOwnedEntryMemoryCost()));

                    totalHeapCost += statM.getHeapCost();

                } else if (distributedObject instanceof CachedQueue) {
                    CachedQueue queue = (CachedQueue) distributedObject;
                    LocalCachedQueueStats statQ = queue.getLocalQueueStats();

                    sb.append("\tsize = " + queue.size());
                    sb.append("\tmax = " + statQ.getCacheMaxSize());
                    sb.append("\tmem = " + bytesToMb(statQ.getHeapCost()));

                    totalHeapCost += statQ.getHeapCost();
                } else if (distributedObject instanceof IExecutorService) {
                    IExecutorService executorService = (IExecutorService) distributedObject;
                    LocalExecutorStats statE = executorService.getLocalExecutorStats();
                    sb.append("\tpending = " + statE.getPendingTaskCount());
                }
            }

            sb.append("\n\nTOTAL Heap Cost = " + bytesToMb(totalHeapCost));
        }

        return sb.toString();
    }

    @Override
    public String getNodeStats() {
        StringBuilder sb = new StringBuilder();

        sb.append("\nMongo Maps statistics (rate per second at last one minute):");
        sb.append(String.format("\ndelete mean: %8.3f rate: %8.3f max: %8.3f",
                MongoMapStore.deleteTimer.mean(), MongoMapStore.deleteTimer.oneMinuteRate(), MongoMapStore
                        .deleteTimer.max()));
        sb.append(String.format("\nload   mean: %8.3f rate: %8.3f max: %8.3f",
                MongoMapStore.loadTimer.mean(), MongoMapStore.loadTimer.oneMinuteRate(), MongoMapStore
                        .loadTimer.max()));
        sb.append(String.format("\nloadS  mean: %8.3f rate: %8.3f max: %8.3f",
                MongoMapStore.loadSuccessTimer.mean(), MongoMapStore.loadSuccessTimer.oneMinuteRate(),
                MongoMapStore.loadSuccessTimer.max()));
        sb.append(String.format("\nstore  mean: %8.3f rate: %8.3f max: %8.3f",
                MongoMapStore.storeTimer.mean(), MongoMapStore.storeTimer.oneMinuteRate(), MongoMapStore
                        .storeTimer.max()));

        sb.append("\nMongo Queues statistics:");
        sb.append(String.format("\ndelete mean: %8.3f rate: %8.3f max: %8.3f",
                MongoCachedQueueStore.deleteTimer.mean(), MongoCachedQueueStore.deleteTimer.oneMinuteRate(),
                MongoCachedQueueStore.deleteTimer.max()));
        sb.append(String.format("\nload   mean: %8.3f rate: %8.3f max: %8.3f",
                MongoCachedQueueStore.loadTimer.mean(), MongoCachedQueueStore.loadTimer.oneMinuteRate(),
                MongoCachedQueueStore.loadTimer.max()));
        sb.append(String.format("\nloadA  mean: %8.3f rate: %8.3f max: %8.3f",
                MongoCachedQueueStore.loadAllTimer.mean(), MongoCachedQueueStore.loadAllTimer.oneMinuteRate()
                , MongoCachedQueueStore.loadAllTimer.max()));
        sb.append(String.format("\nstore  mean: %8.3f rate: %8.3f max: %8.3f",
                MongoCachedQueueStore.storeTimer.mean(), MongoCachedQueueStore.storeTimer.oneMinuteRate(),
                MongoCachedQueueStore.storeTimer.max()));

        sb.append("\n errorsCounter = " + GeneralTaskServer.errorsCounter.get());

        sb.append("\n startedProcessesCounter = " +
                GeneralTaskServer.startedProcessesCounter.get() +
                "  finishedProcessesCounter = " +
                GeneralTaskServer.finishedProcessesCounter.get() +
                "  brokenProcessesCounter = " +
                GeneralTaskServer.brokenProcessesCounter.get());


        sb.append("\n processesOnTimeout = " +
                RecoveryThreadsImpl.processesOnTimeoutFoundedCounter.get() +
                "  recoveredProcesses = " +
                RecoveryServiceImpl.recoveredProcessesCounter.get() +
                "  recoveredTasks = " +
                RecoveryServiceImpl.recoveredTasksCounter.get() +
                "  recoveredInterruptedTasks = " +
                RecoveryServiceImpl.recoveredInterruptedTasksCounter.get() +
                "  restartedBrokenTasks = " +
                RecoveryServiceImpl.restartedBrokenTasks.get() +
                "  recoveredProcessDecisionCounter = " +
                RecoveryServiceImpl.recoveredProcessDecisionCounter.get() +
                "  restartedIncompleteTasksCounter = " +
                RecoveryServiceImpl.restartedIncompleteTasksCounter.get());

        sb.append("\n decisions = " + GeneralTaskServer.receivedDecisionsCounter.get() +
                "  pending = " + (GeneralTaskServer.receivedDecisionsCounter.get()
                - GeneralTaskServer.processedDecisionsCounter.get()));

        sb.append("\n pushed to queue = " + HzQueueService.pushedTaskToQueue.get() +
                "  pending = " + (HzQueueService.pushedTaskToQueue.get() - QueueContainer.addedTaskToQueue.get
                ()) + " with delay = " + HzQueueService.pushedTaskToQueueWithDelay.get() + " backed " +
                MongoStorageFactory.bakedTasks.get());

        sb.append("\n GC: total = " + HzGarbageCollectorService.deletedProcessCounter.get() +
                    " lost processes = " + LostProcessCleaner.cleanedProcessesCounter.get() +
                    " lost graphs = " + LostGraphCleaner.cleanedGraphsCounter.get());

        {
            double release = HzTaskServerMetrics.statRelease.mean();
            double statPdAll = HzTaskServerMetrics.statPdAll.mean();
            double statPdLock = HzTaskServerMetrics.statPdLock.mean();
            double statPdWork = HzTaskServerMetrics.statPdWork.mean();

            sb.append(String.format("\n decision: release = %8.3f process: rate = %8.3f sum = %8.3f lock = " +
                            "%8.3f work = %8.3f unlock = %8.3f maxR = %8.3f  maxD = %8.3f",
                    release, HzTaskServerMetrics.statPdAll.oneMinuteRate(), statPdAll, statPdLock, statPdWork,
                    (statPdAll - statPdLock - statPdWork), HzTaskServerMetrics.statRelease.max(),
                    HzTaskServerMetrics.statPdAll.max()));

        }

        return sb.toString();
    }

    @Override
    public int getFinishedProcessesCounter() {

        Map<Member, Future<Integer>> nodesResults = executorService.submitToAllMembers(new
                FinishedProcessesCounterCallable());

        int sum = 0;

        for (Future<Integer> nodeResultFuture : nodesResults.values()) {
            Integer nodeResult = null;
            try {
                nodeResult = nodeResultFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Can not receive finishedProcessCounter value from node", e);
            }

            if (nodeResult == null) {
                continue;
            }

            sum += nodeResult;
        }

        return sum;
    }


}
