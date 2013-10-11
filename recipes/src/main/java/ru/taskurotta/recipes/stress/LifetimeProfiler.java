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
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: greg
 */
public class LifetimeProfiler extends SimpleProfiler implements ApplicationContextAware {
    private final static Logger log = LoggerFactory.getLogger(LifetimeProfiler.class);

    public static AtomicLong taskCount = new AtomicLong(0);
    public static AtomicLong startTime = new AtomicLong(0);
    public static AtomicLong lastTime = new AtomicLong(0);
    private int tasksForStat = 200;
    private int deltaShot = 3000;

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

                if (taskCount.get() == 0) {
                    long current = System.currentTimeMillis();
                    lastTime.set(current);
                    startTime.set(current);
                }

                Task task = taskSpreader.poll();
                long curTime = System.currentTimeMillis();
                if (null != task) {
                    StressTaskCreator.GLOBAL_LATCH.countDown();
                    long count = taskCount.incrementAndGet();
                    if (count % (StressTaskCreator.getInitialSize() - deltaShot) == 0) {
                        if (StressTaskCreator.LATCH != null) {
                            System.out.println("Shot on " + count);
                            StressTaskCreator.LATCH.countDown();
                        }
                    }
                    if (count % tasksForStat == 0) {
                        double time = 0.001 * (curTime - lastTime.get());
                        double rate = 1000.0D * tasksForStat / (curTime - lastTime.get());
                        System.out.printf("       tasks: %6d; time: %6.3f s; rate: %8.3f tps\n", count, time, rate);
                        lastTime.set(curTime);
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
