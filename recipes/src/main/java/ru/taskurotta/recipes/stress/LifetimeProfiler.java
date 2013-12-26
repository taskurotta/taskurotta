package ru.taskurotta.recipes.stress;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.LocalQueueStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.hazelcast.store.MongoQueueStore;

import java.util.Formatter;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: greg
 */
public class LifetimeProfiler extends SimpleProfiler implements ApplicationContextAware {

    private final static Logger log = LoggerFactory.getLogger(LifetimeProfiler.class);
    public static AtomicLong taskCount = new AtomicLong(0);
    public static AtomicLong startTime = new AtomicLong(0);
    public static AtomicLong lastTime = new AtomicLong(0);
    public static int tasksForStat = 500;
    public static double totalDelta = 0;
    public static AtomicInteger stabilizationCounter = new AtomicInteger(0);
    public static AtomicBoolean stopDecorating = new AtomicBoolean(false);
//    private long nextShot = 0;
    private double deltaRate = 0;
    private double previousRate = 0;
    private boolean timeIsZero = true;
    private int deltaShot = 2000;
    private int maxTaskQuantity = -1;
    private double previousCountTotalRate = 0;
    private double targetTolerance = 4.0;
    private AtomicInteger nullPoll = new AtomicInteger(0);

    public LifetimeProfiler() {
    }

    public LifetimeProfiler(Class actorClass, Properties properties) {

        if (properties.containsKey("tasksForStat")) {
            tasksForStat = (Integer) properties.get("tasksForStat");
        }

        if (properties.containsKey("deltaShot")) {
            deltaShot = (Integer) properties.get("deltaShot");
        }

        if (properties.containsKey("targetTolerance")) {
            targetTolerance = (Double) properties.get("targetTolerance");
        }

        if (properties.containsKey("maxTaskQuantity")) {
            maxTaskQuantity = (Integer) properties.get("maxTaskQuantity");
        }

    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {
            @Override
            public Task poll() {
                if (stopDecorating.get()) {
                    return null;
                }

                try {
                    StressTaskCreator.cd.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }



                Task task = taskSpreader.poll();
                if (task == null) {

                    int localNullPoll = nullPoll.incrementAndGet();

                    if ((localNullPoll + 1) % 1000 == 0) {
                        log.error("Actors still receive empty answer [{}]",  localNullPoll + 1);
                    }
                    return null;
                }

//                if (nextShot == 0) {
//                    nextShot = (StressTaskCreator.getShotSize() * StressTaskCreator.getInitialCount()) - deltaShot;
//                }
                long count = taskCount.incrementAndGet();

                if (count % 5000 == 0) {

                    long totalHeapCost = 0;

                    StringBuilder sb = new StringBuilder();

                    for (HazelcastInstance hzInstance : Hazelcast.getAllHazelcastInstances()) {

                        sb.append("\n============  " + hzInstance.getName() + "  " + "===========");

                        for (DistributedObject distributedObject : hzInstance.getDistributedObjects()) {
                            sb.append("\n" + distributedObject.getServiceName() + " -> " + distributedObject.getName
                                    ());
                            if (distributedObject instanceof IMap) {
                                IMap map = (IMap) distributedObject;
                                LocalMapStats stat = map.getLocalMapStats();

                                sb.append("\tsize = " + map.size());
                                sb.append("\townedEntryMemoryCost = " + bytesToMb(stat.getOwnedEntryMemoryCost()));
                                sb.append("\theapCost = " + bytesToMb(stat.getHeapCost()));
                                sb.append("\tdirtyEntryCount = " + stat.getDirtyEntryCount());

                                totalHeapCost += stat.getHeapCost();
                            }
                            if (distributedObject instanceof IQueue) {
                                IQueue queue = (IQueue) distributedObject;
                                LocalQueueStats stat = queue.getLocalQueueStats();

                                sb.append("\tsize = " + queue.size());
                                sb.append("\townedItemCount = " + stat.getOwnedItemCount());
                            }
                        }

                        sb.append("\n\nTOTAL Heap Cost = " + bytesToMb(totalHeapCost));
                    }

                    sb.append("\nMongo Maps statistics:");
                    sb.append("\ndelete \tmean = " + MongoMapStore.deleteTimer.mean() + " \toneMinuteRate = " +
                            MongoMapStore.deleteTimer.oneMinuteRate());
                    sb.append("\nload \tmean = " + MongoMapStore.loadTimer.mean() + " \toneMinuteRate = " +
                            MongoMapStore.loadTimer.oneMinuteRate());
                    sb.append("\nstore \tmean = " + MongoMapStore.storeTimer.mean() + " \toneMinuteRate = " +
                            MongoMapStore.storeTimer.oneMinuteRate());

                    sb.append("\nMongo Queues statistics:");
                    sb.append("\ndelete \tmean = " + MongoQueueStore.deleteTimer.mean() + " \toneMinuteRate = " +
                            MongoQueueStore.deleteTimer.oneMinuteRate());
                    sb.append("\nload \tmean = " + MongoQueueStore.loadTimer.mean() + " \toneMinuteRate = " +
                            MongoQueueStore.loadTimer.oneMinuteRate());
                    sb.append("\nstore \tmean = " + MongoQueueStore.storeTimer.mean() + " \toneMinuteRate = " +
                            MongoQueueStore.storeTimer.oneMinuteRate());
                    System.err.println(sb);
                }

//                    if (count > 7000) {
//                        try {
//                            TimeUnit.HOURS.sleep(1);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }

//                    if (count == nextShot) {
//                        if (StressTaskCreator.LATCH != null) {
//                            nextShot += StressTaskCreator.getShotSize();
//                            log.info("Shot on " + count);
//                            StressTaskCreator.LATCH.countDown();
//                        }
//                    }

                ThreadLocalRandom tlr = ThreadLocalRandom.current();

                StressTaskCreator.sendOneTask(tlr);


                if (timeIsZero) {
                    long current = System.currentTimeMillis();
                    lastTime.set(current);
                    startTime.set(current);
                    timeIsZero = false;
                }
                long curTime = System.currentTimeMillis();
                if (count % tasksForStat == 0) {
                    double time = 0.001 * (curTime - lastTime.get());
                    double rate = 1000.0D * tasksForStat / (curTime - lastTime.get());
                    if (previousRate != 0) {
                        deltaRate = Math.abs(rate - previousRate);
                        totalDelta += deltaRate;
                    }
                    double totalRate = 1000.0 * count / (double) (LifetimeProfiler.lastTime.get() - LifetimeProfiler.startTime.get());
                    double currentCountTotalRate = count / totalRate;
                    double currentTolerance = ((currentCountTotalRate * 100) / previousCountTotalRate) - 100;
                    previousCountTotalRate = currentCountTotalRate;
                    previousRate = rate;
                    lastTime.set(curTime);

                    if ((maxTaskQuantity > 0 && maxTaskQuantity < count) || (maxTaskQuantity == -1 &&
                            currentTolerance < targetTolerance)) {

                        stabilizationCounter.incrementAndGet();
                    } else {
                        if (stabilizationCounter.get() > 0) {
                            stabilizationCounter.set(0);
                        }
                    }
                    log.info(String.format("       tasks: %6d; time: %6.3f s; rate: %8.3f tps; deltaRate: %8.3f; " +
                            "totalRate: %8.3f; tolerance: %8.3f; freeMemory: %6d;\n", count, time, rate,
                            deltaRate, totalRate, currentTolerance, Runtime.getRuntime().freeMemory() / 1024 / 1024));
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
        return new Formatter().format("%8.2f", ((double) bytes / 1024 / 1024)).toString();
    }

    public static void main(String[] args) {
        System.err.println("ggg: " + bytesToMb(1000000));
        System.err.println("ggg: " + bytesToMb(1000000));
    }

    public void setTargetTolerance(double targetTolerance) {
        this.targetTolerance = targetTolerance;
    }

    public void setTasksForStat(int tasksForStat) {
        LifetimeProfiler.tasksForStat = tasksForStat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
