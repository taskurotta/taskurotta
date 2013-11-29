package ru.taskurotta.recipes.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private long nextShot = 0;
    private double deltaRate = 0;
    private double previousRate = 0;
    private boolean timeIsZero = true;
    private int deltaShot = 2000;

    private double previousCountTotalRate = 0;
    public static AtomicInteger stabilizationCounter = new AtomicInteger(0);
    private double targetTolerance = 4.0;

    private AtomicLong startTimeOfTask = new AtomicLong(0);
    public static AtomicBoolean stopDecorating = new AtomicBoolean(false);

    public static List<Long> arrayOfDuration = new CopyOnWriteArrayList<>();

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
    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {
            @Override
            public Task poll() {
                if (stopDecorating.get()) {
                    return taskSpreader.poll();
                }
                Task task = taskSpreader.poll();
                if (null != task) {

                    if (nextShot == 0) {
                        nextShot = (StressTaskCreator.getShotSize() * StressTaskCreator.getInitialCount()) - deltaShot;
                    }
                    long count = taskCount.incrementAndGet();
                    if (count == nextShot) {
                        if (StressTaskCreator.LATCH != null) {
                            nextShot += StressTaskCreator.getShotSize();
                            log.info("Shot on " + count);
                            StressTaskCreator.LATCH.countDown();
                        }
                    }
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
                        if (currentTolerance < targetTolerance) {
                            stabilizationCounter.incrementAndGet();
                        } else if (stabilizationCounter.get() > 0 && currentTolerance > targetTolerance) {
                            stabilizationCounter.set(0);
                        }
                        log.info(String.format("       tasks: %6d; time: %6.3f s; rate: %8.3f tps; deltaRate: %8.3f; totalRate: %8.3f; tolerance: %8.3f;\n", count, time, rate, deltaRate, totalRate, currentTolerance));
                    }
                }
                collectDataOfEveryTask();
                return task;
            }

            @Override
            public void release(TaskDecision taskDecision) {
                taskSpreader.release(taskDecision);
            }
        };
    }

    private void collectDataOfEveryTask() {
        if (startTimeOfTask.get() == 0) {
            startTimeOfTask.set(System.nanoTime());
        } else {
            long val = System.nanoTime() - startTimeOfTask.get();
            arrayOfDuration.add(val);
            startTimeOfTask.set(System.nanoTime());
        }
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
