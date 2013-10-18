package ru.taskurotta.recipes.stress;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: greg
 */
public class LifetimeProfiler extends SimpleProfiler implements ApplicationContextAware {

    public static AtomicLong taskCount = new AtomicLong(0);
    public static AtomicLong startTime = new AtomicLong(0);
    public static AtomicLong lastTime = new AtomicLong(0);

    public static int tasksForStat = 500;
    public static double totalDelta = 0;

    private int deltaShot = 3000;
    private long nextShot = 6000;
    private double deltaRate = 0;
    private double previousRate = 0;
    private boolean timeIsZero = true;

    private double previousCountTotalRate = 0;
    public static AtomicInteger stabilizationCounter = new AtomicInteger(0);
    private double targetTolerance = 0.5;

    public LifetimeProfiler() {
    }

    public LifetimeProfiler(Class actorClass, Properties properties) {
        if (properties.containsKey("tasksForStat")) {
            tasksForStat = (Integer) properties.get("tasksForStat");
        }
        if (properties.containsKey("deltaShot")) {
            deltaShot = (Integer) properties.get("deltaShot");
        }
    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {
            @Override
            public Task poll() {
                Task task = taskSpreader.poll();
                if (null != task) {
                    long count = taskCount.incrementAndGet();
                    if (count == nextShot) {
                        if (StressTaskCreator.LATCH != null) {
                            nextShot = count + 2000;
                            System.out.println("Shot on " + count);
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
                        System.out.printf("       tasks: %6d; time: %6.3f s; rate: %8.3f tps; deltaRate: %8.3f; totalCount/totalRate: %8.3f; tolerance: %8.3f;\n", count, time, rate, deltaRate, currentCountTotalRate, currentTolerance);
                    }
                }
                return task;
            }

            @Override
            public void release(TaskDecision taskDecision) {
                taskSpreader.release(taskDecision);
            }
        };
    }

    public void setTasksForStat(int tasksForStat) {
        LifetimeProfiler.tasksForStat = tasksForStat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
