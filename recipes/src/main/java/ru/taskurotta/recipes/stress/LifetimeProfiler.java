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

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: greg
 */
public class LifetimeProfiler extends SimpleProfiler implements ApplicationContextAware {
    private final static Logger log = LoggerFactory.getLogger(LifetimeProfiler.class);

    public static AtomicLong taskCount = new AtomicLong(0);
    public static AtomicLong startTime = new AtomicLong(0);
    public static AtomicLong lastTime = new AtomicLong(0);
    public static int tasksForStat = 200;
    private int deltaShot = 3000;
    private double deltaRate = 0;
    private double previousRate = 0;
    public static double totalDelta = 0;
    private static AtomicBoolean timeIsZero = new AtomicBoolean(true);

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
                    StressTaskCreator.GLOBAL_LATCH.countDown();
                    long count = taskCount.incrementAndGet();
                    if (count % (StressTaskCreator.getInitialSize() - deltaShot) == 0) {
                        if (StressTaskCreator.LATCH != null) {
                            System.out.println("Shot on " + count);
                            StressTaskCreator.LATCH.countDown();
                        }
                    }
                    if (!StressTaskCreator.isWarmingUp()) {
                        if (timeIsZero.get()) {
                            long current = System.currentTimeMillis();
                            lastTime.set(current);
                            startTime.set(current);
                            timeIsZero.set(false);
                        }
                        long curTime = System.currentTimeMillis();
                        if (count % tasksForStat == 0) {
                            double time = 0.001 * (curTime - lastTime.get());
                            double rate = 1000.0D * tasksForStat / (curTime - lastTime.get());
                            if (previousRate != 0) {
                                deltaRate = Math.abs(rate - previousRate);
                                totalDelta += deltaRate;
                            }
                            System.out.printf("       tasks: %6d; time: %6.3f s; rate: %8.3f tps; deltaRate: %8.3f\n", count, time, rate, deltaRate);
                            previousRate = rate;
                            lastTime.set(curTime);
                        }
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
        this.tasksForStat = tasksForStat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
