package ru.taskurotta.test.stress;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
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
import ru.taskurotta.service.hz.queue.HzQueueService;
import ru.taskurotta.service.recovery.DefaultIncompleteProcessFinder;
import ru.taskurotta.service.recovery.GeneralRecoveryProcessService;
import ru.taskurotta.test.stress.process.Starter;
import ru.taskurotta.test.stress.util.DaemonThread;
import ru.taskurotta.util.metrics.HzTaskServerMetrics;

import java.util.Formatter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ProcessPusher {

    private final static Logger logger = LoggerFactory.getLogger(ProcessPusher.class);

    public static AtomicInteger counter = new AtomicInteger(0);

    // per second

    public ProcessPusher(final Starter starter, final HazelcastInstance hazelcastInstance, final int maxProcessQuantity,
                         final int startSpeedPerSecond, final int threadCount, final int minQueuesSize,
                         final int maxQueuesSize, final int waitAfterDoneSeconds) {

        final Queue queue = new ConcurrentLinkedQueue();

        // start planner thread
        new DaemonThread("process planner", TimeUnit.SECONDS, 1) {

            boolean fixedPushRate = false;
            int currentSpeedPerSecond = startSpeedPerSecond;
            //            int currentSpeedPerSecond = 10000;
            final long startTime = System.currentTimeMillis();

            @Override
            public void daemonJob() {


                int sumQueuesSize = getSumQueuesSize(hazelcastInstance);

                // should waiting to prevent overload
                if (!fixedPushRate && sumQueuesSize > maxQueuesSize) {

                    // go slowly
                    currentSpeedPerSecond--;
                    return;
                }


                if (!fixedPushRate && sumQueuesSize < minQueuesSize) {

                    // go faster
                    currentSpeedPerSecond++;
                }

                int currSize = queue.size();

                if (currSize < currentSpeedPerSecond) {

                    int actualSpeed = currentSpeedPerSecond;

                    int needToPush = actualSpeed - currSize;

                    logger.info("Speed pps: planned {}, actual {}. start new {}", actualSpeed,
                            (int) (1D * counter.get() / ((System.currentTimeMillis() - startTime) / 1000)), needToPush);

                    double interval = 1000l / actualSpeed;
                    double timeCursor = System.currentTimeMillis();

                    for (int i = 0; i < needToPush; i++) {
                        timeCursor += interval;

                        queue.add((long) (timeCursor));

                        if (counter.incrementAndGet() == maxProcessQuantity) {
                            throw new StopSignal();
                        }
                    }

                    return;
                }

            }

            private int getSumQueuesSize(HazelcastInstance hazelcastInstance) {

                int sum = 0;

                for (DistributedObject distributedObject : hazelcastInstance.getDistributedObjects()) {
                    if (distributedObject instanceof CachedQueue) {
                        Queue queue = (CachedQueue) distributedObject;
                        sum += queue.size();
                    }
                }

                return sum;
            }

        }.start();


        // start terminator thread
        new DaemonThread("test terminator", TimeUnit.SECONDS, 1) {

            @Override
            public void daemonJob() {

                if (counter.get() >= maxProcessQuantity &&
                        GeneralTaskServer.finishedProcessesCounter.get() >= maxProcessQuantity) {
                    // stop JVM

                    logger.info("Done... Waiting before exit {} seconds", waitAfterDoneSeconds);
                    try {
                        TimeUnit.SECONDS.sleep(waitAfterDoneSeconds);
                    } catch (InterruptedException e) {
                        // ignore
                    }


                    System.exit(0);
                }
            }

        }.start();


        // start pusher threads
        for (int i = 0; i < threadCount; i++) {

            new DaemonThread("process pusher " + i, null, 0) {

                @Override
                public void daemonJob() {

                    Long timeToStart = (Long) queue.poll();

                    if (timeToStart == null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                        }

                        return;
                    }

                    long currTime = System.currentTimeMillis();

                    if (currTime < timeToStart.longValue()) {

                        try {
                            TimeUnit.MILLISECONDS.sleep(timeToStart.longValue() - currTime);
                        } catch (InterruptedException e) {
                        }
                    }

                    starter.start();
                }
            }.start();

        }


        // start dump thread
        new DaemonThread("stats dumper", TimeUnit.SECONDS, 5) {

            @Override
            public void daemonJob() {

                long totalHeapCost = 0;

                StringBuilder sb = new StringBuilder();

                for (HazelcastInstance hzInstance : Hazelcast.getAllHazelcastInstances()) {

                    sb.append("\n============  ").append(hzInstance.getName()).append("  ===========");

                    for (DistributedObject distributedObject : hzInstance.getDistributedObjects()) {
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

                int pushedCount = ProcessPusher.counter.get();
                int startedCount = GeneralTaskServer.startedProcessesCounter.get();

                sb.append("\n Processes: pushed = " + pushedCount +
                        "  started = " + startedCount +
                        "  delta = " + (pushedCount - startedCount) +
                        "  finished = " +
                        GeneralTaskServer.finishedProcessesCounter.get() +
                        "  broken = " +
                        GeneralTaskServer.brokenProcessesCounter.get() +
                        "  resurrected = " + GeneralRecoveryProcessService.resurrectedProcessesCounter.get());

                sb.append("\n processesOnTimeout = " +
                        DefaultIncompleteProcessFinder.processesOnTimeoutFoundedCounter.get() +
                        "  restartedProcesses = " +
                        GeneralRecoveryProcessService.restartedProcessesCounter.get() +
                        "  restartedTasks = " +
                        GeneralRecoveryProcessService.restartedTasksCounter);

                sb.append("\n startedDistributedTasks = " + GeneralTaskServer.startedDistributedTasks.get() +
                        "  pending = " + (GeneralTaskServer.startedDistributedTasks.get()
                        - GeneralTaskServer.finishedDistributedTasks.get()));

                sb.append("\n pushed to queue = " + HzQueueService.pushedTaskToQueue.get() +
                        "  pending = " + (HzQueueService.pushedTaskToQueue.get() - QueueContainer.addedTaskToQueue.get
                        ()) + " with delay = " + HzQueueService.pushedTaskToQueueWithDelay.get() + " backed " +
                        MongoStorageFactory.bakedTasks.get());

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

                logger.info(sb.toString());


            }

        }.start();
    }

    public static String bytesToMb(long bytes) {
        return new Formatter().format("%6.2f", ((double) bytes / 1024 / 1024)).toString();
    }

}
