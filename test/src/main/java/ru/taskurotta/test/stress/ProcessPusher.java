package ru.taskurotta.test.stress;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.LocalCachedQueueStats;
import ru.taskurotta.hazelcast.queue.store.mongodb.MongoCachedQueueStore;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.service.recovery.DefaultIncompleteProcessFinder;
import ru.taskurotta.service.recovery.GeneralRecoveryProcessService;
import ru.taskurotta.test.stress.process.Starter;
import ru.taskurotta.test.stress.util.DaemonThread;

import java.util.Formatter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ProcessPusher {

    private final static Logger logger = LoggerFactory.getLogger(LifetimeProfiler.class);

    public static AtomicInteger counter = new AtomicInteger(0);

    // per second

    public ProcessPusher(final Starter starter, final HazelcastInstance hazelcastInstance, final int maxProcessQuantity,
                         final int initialProcessPresSecondPush, final int threadCount, final int minQueuesSize,
                         final int maxQueuesSize) {

        final Queue queue = new ConcurrentLinkedQueue();

        // start planner thread
        new DaemonThread("process planner", TimeUnit.SECONDS, 1) {

            int currentSpeedPerSecond = initialProcessPresSecondPush;

            @Override
            public void daemonJob() {


                int sumQueuesSize = getSumQueuesSize(hazelcastInstance);

                // should waiting to prevent overload
                if (sumQueuesSize > maxQueuesSize) {

                    // go slowly
                    currentSpeedPerSecond--;
                    return;
                }


                if (sumQueuesSize < minQueuesSize) {

                    // go faster
                    currentSpeedPerSecond++;
                }

                int currSize = queue.size();

                if (currSize < currentSpeedPerSecond) {

                    int actualSpeed = currentSpeedPerSecond;

                    int needToPush = actualSpeed - currSize;

                    logger.info("Speed pps = " + actualSpeed);

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

            int currentSpeedPerSecond = initialProcessPresSecondPush;

            @Override
            public void daemonJob() {

                if (counter.get() >= maxProcessQuantity &&
                        GeneralTaskServer.finishedProcessesCounter.get() >= maxProcessQuantity) {
                    // stop JVM
                    try {
                        TimeUnit.SECONDS.sleep(5);
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
                            sb.append("\townedEntryMemoryCost = " + bytesToMb(statM.getOwnedEntryMemoryCost()));
                            sb.append("\theapCost = " + bytesToMb(statM.getHeapCost()));
                            sb.append("\tdirtyEntryCount = " + statM.getDirtyEntryCount());

                            totalHeapCost += statM.getHeapCost();

                        } else if (distributedObject instanceof CachedQueue) {
                            CachedQueue queue = (CachedQueue) distributedObject;
                            LocalCachedQueueStats statQ = queue.getLocalQueueStats();

                            sb.append("\tsize = " + queue.size());
                            sb.append("\tcacheSize = " + statQ.getCacheSize());
                            sb.append("\tcacheMaxSize = " + statQ.getCacheMaxSize());
                            sb.append("\theapCost = " + bytesToMb(statQ.getHeapCost()));

                            totalHeapCost += statQ.getHeapCost();
                        }
                    }

                    sb.append("\n\nTOTAL Heap Cost = " + bytesToMb(totalHeapCost));
                }

                sb.append("\nMongo Maps statistics:");
                sb.append(String.format("\ndelete mean: %8.3f oneMinuteRate: %8.3f",
                        MongoMapStore.deleteTimer.mean(), MongoMapStore.deleteTimer.oneMinuteRate()));
                sb.append(String.format("\nload   mean: %8.3f oneMinuteRate: %8.3f",
                        MongoMapStore.loadTimer.mean(), MongoMapStore.loadTimer.oneMinuteRate()));
                sb.append(String.format("\nload success   mean: %8.3f oneMinuteRate: %8.3f",
                        MongoMapStore.loadSuccessTimer.mean(), MongoMapStore.loadSuccessTimer.oneMinuteRate()));
                sb.append(String.format("\nstore  mean: %8.3f oneMinuteRate: %8.3f",
                        MongoMapStore.storeTimer.mean(), MongoMapStore.storeTimer.oneMinuteRate()));

                sb.append("\nMongo Queues statistics:");
                sb.append(String.format("\ndelete mean: %8.3f oneMinuteRate: %8.3f",
                        MongoCachedQueueStore.deleteTimer.mean(), MongoCachedQueueStore.deleteTimer.oneMinuteRate()));
                sb.append(String.format("\nload   mean: %8.3f oneMinuteRate: %8.3f",
                        MongoCachedQueueStore.loadTimer.mean(), MongoCachedQueueStore.loadTimer.oneMinuteRate()));
                sb.append(String.format("\nload all   mean: %8.3f oneMinuteRate: %8.3f",
                        MongoCachedQueueStore.loadAllTimer.mean(), MongoCachedQueueStore.loadAllTimer.oneMinuteRate()));
                sb.append(String.format("\nstore  mean: %8.3f oneMinuteRate: %8.3f",
                        MongoCachedQueueStore.storeTimer.mean(), MongoCachedQueueStore.storeTimer.oneMinuteRate()));

                int pushedCount = ProcessPusher.counter.get();
                int startedCount = GeneralTaskServer.startedProcessesCounter.get();

                sb.append("\n pushedProcessCounter = " + pushedCount +
                        "  startedProcessesCounter = " + startedCount +
                        "  delta = " + (pushedCount - startedCount) +
                        "  finishedProcessesCounter = " +
                        GeneralTaskServer.finishedProcessesCounter.get() +
                        "  brokenProcessesCounter = " +
                        GeneralTaskServer.brokenProcessesCounter.get());

                sb.append("\n processesOnTimeoutFoundedCounter = " +
                        DefaultIncompleteProcessFinder.processesOnTimeoutFoundedCounter.get() +
                        "  restartedProcessesCounter = " +
                        GeneralRecoveryProcessService.restartedProcessesCounter.get() +
                        "  restartedTasksCounter = " +
                        GeneralRecoveryProcessService.restartedTasksCounter);


                logger.info(sb.toString());
            }

        }.start();
    }

    public static String bytesToMb(long bytes) {
        return new Formatter().format("%6.2f", ((double) bytes / 1024 / 1024)).toString();
    }

}
