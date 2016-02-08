package ru.taskurotta.test.stress;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalExecutorStats;
import com.hazelcast.monitor.LocalMapStats;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.LocalCachedQueueStats;
import ru.taskurotta.hazelcast.queue.delay.MongoStorageFactory;
import ru.taskurotta.hazelcast.queue.impl.QueueContainer;
import ru.taskurotta.hazelcast.queue.store.mongodb.MongoCachedQueueStore;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.service.hz.queue.HzQueueService;
import ru.taskurotta.service.hz.storage.StringSetCounter;
import ru.taskurotta.service.recovery.impl.RecoveryServiceImpl;
import ru.taskurotta.service.recovery.impl.RecoveryThreadsImpl;
import ru.taskurotta.test.stress.process.Starter;
import ru.taskurotta.util.DaemonThread;
import ru.taskurotta.util.metrics.HzTaskServerMetrics;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ProcessPusher {

    private final static Logger logger = LoggerFactory.getLogger(ProcessPusher.class);

    public static int taskPerSecondSpeed = 0;

    private static class ProcessToStartItem {

        public long timeToStart;
        public String customId;

        public ProcessToStartItem(long timeToStart, String customId) {
            this.timeToStart = timeToStart;
            this.customId = customId;
        }
    }

    //with default finished processes counter, only per node
    public ProcessPusher(final Starter starter, final HazelcastInstance hazelcastInstance, final int maxProcessQuantity,
                         final int startSpeedPerSecond, final int threadCount, final int minQueuesSize,
                         final int maxQueuesSize, final int waitAfterDoneSeconds, final boolean fixedPushRate) {
        this(starter, hazelcastInstance, maxProcessQuantity, startSpeedPerSecond, threadCount, minQueuesSize,
                maxQueuesSize, waitAfterDoneSeconds, fixedPushRate, new SameJVMStringSetCounter());
    }

    public ProcessPusher(final Starter starter, final HazelcastInstance hazelcastInstance, final int maxProcessQuantity,
                         final int startSpeedPerSecond, final int threadCount, final int minQueuesSize,
                         final int maxQueuesSize, final int waitAfterDoneSeconds, final boolean fixedPushRate, final
                         StringSetCounter stringSetCounter) {

        final Queue<ProcessToStartItem> queue = new ConcurrentLinkedQueue<ProcessToStartItem>();

        final long startTestTime = System.currentTimeMillis();

        final AtomicInteger plannedTaskCounter = new AtomicInteger(0);
        final AtomicInteger startedTaskCounter = new AtomicInteger(0);
        final AtomicInteger finishedTaskCounter = new AtomicInteger(0);
        final AtomicInteger currentSpeedPerSecond = new AtomicInteger(startSpeedPerSecond);

        // start planner thread
        new DaemonThread("process planner", TimeUnit.SECONDS, 1) {

            @Override
            public void daemonJob() {

                if (!fixedPushRate) {

                    int finishedTaskSize = (int) stringSetCounter.getSize();
                    finishedTaskCounter.set(finishedTaskSize);

                    // should be waiting to prevent overload
                    int processInWorkSize = (plannedTaskCounter.get() - finishedTaskSize);
                    if (processInWorkSize > maxQueuesSize) {

                        // go slowly
                        currentSpeedPerSecond.decrementAndGet();
                        return;
                    }


                    if (processInWorkSize < minQueuesSize) {

                        // go faster
                        currentSpeedPerSecond.incrementAndGet();
                    }
                }


                int currSize = queue.size();

                if (currSize < currentSpeedPerSecond.get()) {

                    int actualSpeed = currentSpeedPerSecond.get();

                    int needToPush = actualSpeed - currSize;

                    double interval = 1000l / actualSpeed;
                    double timeCursor = System.currentTimeMillis();

                    for (int i = 0; i < needToPush; i++) {
                        timeCursor += interval;

                        int currentTaskId = plannedTaskCounter.incrementAndGet();

                        ProcessToStartItem processToStartItem = new ProcessToStartItem((long) (timeCursor),
                                createCustomId(currentTaskId));

                        queue.add(processToStartItem);

                        if (currentTaskId == maxProcessQuantity) {
                            taskPerSecondSpeed = (int) (1000.0 * LifetimeProfiler.taskCount
                                    .incrementAndGet() / (double) (System.currentTimeMillis() - LifetimeProfiler
                                    .startRateTime.get()));

                            throw new StopSignal();
                        }
                    }

                    return;
                }

            }

        }.start();


        // start info thread
        new DaemonThread("info", TimeUnit.SECONDS, 5) {

            @Override
            public void daemonJob() {
                logger.info("process pusher: started {} finished {} planned {} queue.size {}",
                        startedTaskCounter.get(), finishedTaskCounter.get(), plannedTaskCounter.get(), queue.size());
            }

        }.start();


        // start terminator thread
        new DaemonThread("test terminator", TimeUnit.SECONDS, 5) {

            @Override
            public void daemonJob() {

                if (plannedTaskCounter.get() < maxProcessQuantity) {
                    return;
                }

                int finishedTaskSize = (int) stringSetCounter.getSize();
                finishedTaskCounter.set(finishedTaskSize);

                if (finishedTaskSize >= maxProcessQuantity) {
                    // stop JVM

                    List<String> supposedUniqueList = new ArrayList<>();
                    for (int i = 1; i <= maxProcessQuantity; i++) {
                        supposedUniqueList.add(createCustomId(i));
                    }

                    List<String> uniqueCustomIds = stringSetCounter.findUniqueItems(supposedUniqueList);
                    if (uniqueCustomIds.size() != 0) {
                        for (String customId: uniqueCustomIds) {
                            logger.error("Process with customId = '{}' not finished", customId);
                        }
                    } else {
                        logger.info("All processes are finished");
                    }

                    logger.info("Done... Total process speed is {} pps. Task speed at the all process pushed " +
                                    "moment is {} tps. Waiting before exit {} seconds",
                            1f * maxProcessQuantity / (System.currentTimeMillis() - startTestTime) * 1000
                            , taskPerSecondSpeed, waitAfterDoneSeconds);
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

                    ProcessToStartItem processToStartItem = queue.poll();

                    if (processToStartItem == null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                        }

                        return;
                    }

                    long currTime = System.currentTimeMillis();

                    if (currTime < processToStartItem.timeToStart) {

                        try {
                            TimeUnit.MILLISECONDS.sleep(processToStartItem.timeToStart - currTime);
                        } catch (InterruptedException e) {
                        }
                    }

                    try {
                        starter.start(processToStartItem.customId);
                        startedTaskCounter.incrementAndGet();
                    } catch (Throwable ex) {
                        logger.error("Cannot start process. Try it later", ex);

                        processToStartItem.timeToStart = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(3);
                        queue.offer(processToStartItem);
                    }
                }
            }.start();

        }


        if (hazelcastInstance != null) {

            // start dump thread
            new DaemonThread("stats dumper", TimeUnit.SECONDS, 5) {

                @Override
                public void daemonJob() {

                    long totalHeapCost = 0;

                    StringBuilder sb = new StringBuilder();

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

                    int pushedCount = plannedTaskCounter.get();
                    int startedCount = GeneralTaskServer.startedProcessesCounter.get();

                    sb.append("\n Processes: pushed = " + pushedCount +
                            "  started = " + startedCount +
                            "  delta = " + (pushedCount - startedCount) +
                            "  finished = " +
                            stringSetCounter.getSize() +
                            "  broken tasks = " +
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
                            "  recoveredProcessDecision = " +
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
    }

    @NotNull
    private String createCustomId(int i) {
        return "" + i;
    }

    public static String bytesToMb(long bytes) {
        return new Formatter().format("%6.2f", ((double) bytes / 1024 / 1024)).toString();
    }


    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: ru.taskurotta.test.stress.ProcessPusher <spring context XML location>");
        } else {
            AbstractApplicationContext ctx = new ClassPathXmlApplicationContext(args);
            logger.info("Pusher application context created");
        }
    }

}
