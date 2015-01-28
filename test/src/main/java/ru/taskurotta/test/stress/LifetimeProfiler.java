package ru.taskurotta.test.stress;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.LocalCachedQueueStats;
import ru.taskurotta.hazelcast.queue.store.mongodb.MongoCachedQueueStore;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.service.recovery.DefaultIncompleteProcessFinder;
import ru.taskurotta.service.recovery.GeneralRecoveryProcessService;
import ru.taskurotta.test.stress.util.DaemonThread;

import java.util.Formatter;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: greg
 */
public class LifetimeProfiler extends SimpleProfiler implements ApplicationContextAware {

    private final static Logger logger = LoggerFactory.getLogger(LifetimeProfiler.class);

    private final static String MAX_PROCESS_QUANTITY = "maxProcessQuantity";
    private final static String TASKS_FOR_STAT = "tasksForStat";


    public static AtomicLong taskCount = new AtomicLong(0);
    public static AtomicLong startedProcessCounter = new AtomicLong(0);
    public static AtomicLong startTime = new AtomicLong(0);
    public static AtomicLong lastTime = new AtomicLong(0);

    public static int tasksForStat = 1000;
    private boolean isFirstPoll = true;
    private int maxProcessQuantity = -1;
    private AtomicInteger nullPoll = new AtomicInteger(0);

    public LifetimeProfiler() {
    }

    public LifetimeProfiler(Class actorClass, Properties properties) {

        String sys = null;

        if (properties.containsKey(TASKS_FOR_STAT)) {
            tasksForStat = (Integer) properties.get(TASKS_FOR_STAT);
        }

        sys = System.getProperty(MAX_PROCESS_QUANTITY);
        if (sys != null) {
            maxProcessQuantity = Integer.valueOf(sys);
        } else {
            if (properties.containsKey(MAX_PROCESS_QUANTITY)) {
                maxProcessQuantity = (Integer) properties.get(MAX_PROCESS_QUANTITY);
            }
        }

    }


    public static AtomicBoolean isReady = new AtomicBoolean(false);

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {

        Set allHazelcastInstances = Hazelcast.getAllHazelcastInstances();

        final HazelcastInstance hazelcastInstance = allHazelcastInstances.size() == 1 ?
                (HazelcastInstance) Hazelcast.getAllHazelcastInstances().toArray()[0] : null;

//        if (hazelcastInstance != null && isReady.compareAndSet(false, true)) {
//            new ProcessPusher(hazelcastInstance, maxProcessQuantity, 50, 10, 4000, 8000);
//        }


        // start dump thread
        new DaemonThread("stat dumper", TimeUnit.SECONDS, 5) {

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
                            LocalMapStats stat = map.getLocalMapStats();

                            sb.append("\tsize = " + map.size());
                            sb.append("\townedEntryMemoryCost = " + bytesToMb(stat.getOwnedEntryMemoryCost()));
                            sb.append("\theapCost = " + bytesToMb(stat.getHeapCost()));
                            sb.append("\tdirtyEntryCount = " + stat.getDirtyEntryCount());

                            totalHeapCost += stat.getHeapCost();
                        }

                        if (distributedObject instanceof CachedQueue) {
                            CachedQueue queue = (CachedQueue) distributedObject;
                            LocalCachedQueueStats stat = queue.getLocalQueueStats();

                            sb.append("\tsize = " + queue.size());
                            sb.append("\tcacheSize = " + stat.getCacheSize());
                            sb.append("\tcacheMaxSize = " + stat.getCacheMaxSize());
                            sb.append("\theapCost = " + bytesToMb(stat.getHeapCost()));

                            totalHeapCost += stat.getHeapCost();
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

        return new TaskSpreader() {
            @Override
            public Task poll() {

                Task task = taskSpreader.poll();
                if (task == null) {

                    int localNullPoll = nullPoll.incrementAndGet();

                    if ((localNullPoll + 1) % 1000 == 0) {
                        logger.error("Actors still receive empty answer [{}]", localNullPoll + 1);
                    }
                    return null;
                }

                long count = taskCount.incrementAndGet();

                // if server not in the same jvm
                if (hazelcastInstance == null) {

                    if (task.getTarget().getName().equals(ru.taskurotta.test.fullfeature.decider.FullFeatureDecider.class
                            .getName()) &&
                            task.getTarget().getMethod().equals("logResult")) {

                        StressTaskCreator.sendOneTask();
                    }
                }


                if (isFirstPoll) {
                    long current = System.currentTimeMillis();
                    lastTime.set(current);
                    startTime.set(current);
                    isFirstPoll = false;
                }

                long curTime = System.currentTimeMillis();
                if (count % tasksForStat == 0) {
                    double time = 0.001 * (curTime - lastTime.get());
                    double rate = 1000.0D * tasksForStat / (curTime - lastTime.get());
                    double totalRate = 1000.0 * count / (double) (curTime - LifetimeProfiler.startTime.get());
                    lastTime.set(curTime);

                    logger.info(String.format("       tasks: %6d; time: %6.3f s; rate: %8.3f tps; " +
                                    "totalRate: %8.3f; freeMemory: %6d;\n", count, time, rate,
                            totalRate, Runtime.getRuntime().freeMemory() / 1024 / 1024));
                }

                return task;
            }

            @Override
            public void release(TaskDecision taskDecision) {
                taskSpreader.release(taskDecision);
            }
        };
    }

    public static String bytesToMb(long bytes) {
        return new Formatter().format("%6.2f", ((double) bytes / 1024 / 1024)).toString();
    }

    public void setTasksForStat(int tasksForStat) {
        LifetimeProfiler.tasksForStat = tasksForStat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
